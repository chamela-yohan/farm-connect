package lk.farmconnect.order.service;

import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.service.StorageService;
import lk.farmconnect.order.dto.AddToCartRequest;
import lk.farmconnect.order.dto.CartItemResponse;
import lk.farmconnect.order.dto.CartResponse;
import lk.farmconnect.order.entity.Cart;
import lk.farmconnect.order.entity.CartItem;
import lk.farmconnect.order.exception.CartException;
import lk.farmconnect.order.exception.InvalidQuantityException;
import lk.farmconnect.order.exception.ProductUnavailableException;
import lk.farmconnect.order.mapper.CartMapper;
import lk.farmconnect.order.repository.CartRepository;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.entity.ProductStatus;
import lk.farmconnect.product.entity.ProductType;
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;
    private final StorageService fileStorageService;

    @Transactional(readOnly = true)
    public CartResponse getCart(User buyer) {
        Cart cart = cartRepository.findByBuyer(buyer).orElseGet(() -> Cart.builder().buyer(buyer).build());
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(User buyer, AddToCartRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductUnavailableException("Product not found"));

        if (product.getFarmer().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot add your own products to the cart.");
        }

        validateProductAvailability(product, request.quantity());

        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseGet(() -> cartRepository.save(Cart.builder().buyer(buyer).build()));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);

        BigDecimal newQuantity = request.quantity();
        if (existingItem != null) {
            newQuantity = existingItem.getQuantity().add(request.quantity());
            existingItem.setQuantity(newQuantity);
            validateProductAvailability(product, newQuantity); // Re-validate against total
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .build();
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        log.info("Added {} of {} to cart for user {}", request.quantity(), product.getTitle(), buyer.getEmail());

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(User buyer, UUID itemId, BigDecimal newQuantity) {
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new CartException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartException("Item not found in cart"));


        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            cart.getItems().remove(item);
        } else {
            // Validate the new quantity against product rules (stock, min/max, step)
            validateProductAvailability(item.getProduct(), newQuantity);
            item.setQuantity(newQuantity);
        }

        cartRepository.save(cart);
        log.info("Updated quantity of {} to {} for user {}", item.getProduct().getTitle(), newQuantity, buyer.getEmail());
        return buildCartResponse(cart);
    }

    @Transactional
    public void removeItem(User buyer, UUID itemId) {
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new CartException("Cart not found"));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new CartException("Item not found in cart");
        }
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(User buyer) {
        Cart cart = cartRepository.findByBuyer(buyer).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    // ==========================================
    // PRIVATE HELPERS & JSONB EXTRACTION
    // ==========================================

    // Safely extract BigDecimal from JSONB attributes
    private BigDecimal getAttributeAsDecimal(Product product, String key) {
        if (product.getAttributes() != null && product.getAttributes().has(key)) {
            try {
                return new BigDecimal(product.getAttributes().get(key).asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // Safely extract LocalDate from JSONB attributes
    private LocalDate getAttributeAsDate(Product product, String key) {
        if (product.getAttributes() != null && product.getAttributes().has(key)) {
            try {
                return LocalDate.parse(product.getAttributes().get(key).asText());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void validateProductAvailability(Product product, BigDecimal requestedQty) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductUnavailableException("Product is no longer available.");
        }

        // ==========================================
        // 1. MIN / MAX VALIDATION
        // ==========================================
        BigDecimal minQty = product.getMinOrderQty() != null ? product.getMinOrderQty() : BigDecimal.ONE;

        if (requestedQty.compareTo(minQty) < 0) {
            throw new InvalidQuantityException("Minimum order quantity is " + minQty);
        }

        if (product.getMaxOrderQty() != null && requestedQty.compareTo(product.getMaxOrderQty()) > 0) {
            throw new InvalidQuantityException("Maximum order quantity is " + product.getMaxOrderQty());
        }

        // ==========================================
        // 2. STEP VALIDATION (The Math Fix)
        // ==========================================
        if (product.getQtyStep() != null && product.getQtyStep().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal step = product.getQtyStep();

            // Calculate the difference from the minimum quantity
            BigDecimal diff = requestedQty.subtract(minQty);

            // Check if the difference is a multiple of the step
            // We divide and check if the result is effectively an integer
            BigDecimal division = diff.divide(step, 4, java.math.RoundingMode.HALF_UP);
            BigDecimal roundedDivision = division.setScale(0, java.math.RoundingMode.HALF_UP);

            // Allow a tiny margin of error for floating point math (e.g., 1.5000000000000002)
            boolean isMultiple = division.subtract(roundedDivision).abs().compareTo(new BigDecimal("0.0001")) <= 0;

            if (!isMultiple) {
                String unit = "";
                if (product.getAttributes() != null && product.getAttributes().has("unit")) {
                    unit = " " + product.getAttributes().get("unit").asText();
                }

                // Generate helpful examples for the error message
                BigDecimal nextValid = minQty.add(step);
                BigDecimal nextNextValid = minQty.add(step.multiply(new BigDecimal("2")));

                throw new InvalidQuantityException(
                        String.format("Quantity must be at least %s, in increments of %s%s (e.g., %s, %s, %s...)",
                                minQty, step, unit, minQty, nextValid, nextNextValid)
                );
            }
        }

        // ==========================================
        // 3. STOCK & EXPIRY (PHYSICAL GOODS ONLY)
        // ==========================================
        if (product.getProductType() == ProductType.PHYSICAL_GOOD) {
            LocalDate expiryDate = getAttributeAsDate(product, "expiryDate");
            if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
                throw new ProductUnavailableException("Product has expired.");
            }

            BigDecimal availableStock = getAttributeAsDecimal(product, "availableStock");
            if (availableStock != null && availableStock.compareTo(requestedQty) < 0) {
                throw new ProductUnavailableException("Insufficient stock available.");
            }
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::buildCartItemResponse) // Use custom builder
                .toList();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(itemResponses, totalAmount, itemResponses.size());
    }

    // Custom builder to handle Presigned URLs and JSONB safely
    private CartItemResponse buildCartItemResponse(CartItem item) {
        Product p = item.getProduct();

        // 1. Map Images to Presigned URLs
        List<String> urls = p.getImages() != null
                ? p.getImages().stream()
                .sorted((i1, i2) -> Integer.compare(i1.getDisplayOrder(), i2.getDisplayOrder()))
                .map(img -> fileStorageService.getPresignedUrl(img.getImageUrl())) // Adjust if you use extractKey()
                .filter(Objects::nonNull)
                .toList()
                : Collections.emptyList();

        // 2. Extract JSONB attributes safely
        BigDecimal stock = getAttributeAsDecimal(p, "availableStock");

        // 3. Build Response with all trading rules
        return new CartItemResponse(
                item.getId(),
                p.getId(),
                p.getTitle(),
                p.getProductType().name(),
                p.getPrice(),
                item.getQuantity(),
                p.getPrice().multiply(item.getQuantity()),
                urls,
                p.getQtyStep(),
                p.getMinOrderQty(),
                p.getMaxOrderQty(),
                stock
        );
    }
}