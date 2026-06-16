package lk.farmconnect.order.service;

import lk.farmconnect.common.exception.BusinessException;
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
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartResponse getCart(User buyer) {
        Cart cart = cartRepository.findByBuyer(buyer).orElseGet(() -> Cart.builder().buyer(buyer).build());
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(User buyer, AddToCartRequest request) {
        // Fetch and Validate Product
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductUnavailableException("Product not found"));

        if (product.getFarmer().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot add your own products to the cart.");
        }

        validateProductAvailability(product, request.quantity());

        //  Find or Create Cart
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseGet(() -> cartRepository.save(Cart.builder().buyer(buyer).build()));

        //  Check if item already exists in cart
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
    // PRIVATE HELPERS & VALIDATION
    // ==========================================

    private void validateProductAvailability(Product product, BigDecimal requestedQty) {
        // Check Status
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductUnavailableException("Product is no longer available.");
        }

        // Check Expiry
        if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(LocalDate.now())) {
            throw new ProductUnavailableException("Product has expired.");
        }

        // Check Stock
        if (product.getAvailableStock() == null || product.getAvailableStock().compareTo(requestedQty) < 0) {
            throw new ProductUnavailableException("Insufficient stock available.");
        }

        // Check Min/Max/Step Rules
        if (product.getMinOrderQty() != null && requestedQty.compareTo(product.getMinOrderQty()) < 0) {
            throw new InvalidQuantityException("Minimum order quantity is " + product.getMinOrderQty());
        }
        if (product.getMaxOrderQty() != null && requestedQty.compareTo(product.getMaxOrderQty()) > 0) {
            throw new InvalidQuantityException("Maximum order quantity is " + product.getMaxOrderQty());
        }
        if (product.getQtyStep() != null && product.getQtyStep().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainder = requestedQty.remainder(product.getQtyStep());
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                throw new InvalidQuantityException("Quantity must be in multiples of " + product.getQtyStep());
            }
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(cartMapper::toCartItemResponse)
                .toList();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.size();

        return cartMapper.toCartResponse(itemResponses, totalAmount, totalItems);
    }
}