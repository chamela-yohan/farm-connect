package lk.farmconnect.order.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.order.dto.*;
import lk.farmconnect.order.entity.*;
import lk.farmconnect.order.event.OrderAcceptedEvent;
import lk.farmconnect.order.event.OrderRejectedEvent;
import lk.farmconnect.order.mapper.OrderMapper;
import lk.farmconnect.order.repository.CartRepository;
import lk.farmconnect.order.repository.OrderRepository;
import lk.farmconnect.order.repository.OrderStatusHistoryRepository;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.entity.ProductType;
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderMapper orderMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // ==========================================
    // CHECKOUT (Cart -> Split Orders)
    // ==========================================
    @Transactional
    public List<OrderResponse> checkout(User buyer, OrderCreateRequest request) {
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new BusinessException("Cart is empty or not found."));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cannot checkout with an empty cart.");
        }

        if (request.deliveryMethod() == DeliveryMethod.DELIVERY) {
            boolean allSupportDelivery = cart.getItems().stream()
                    .allMatch(item -> item.getProduct().isDeliveryAvailable());
            if (!allSupportDelivery) {
                throw new BusinessException("Some items in your cart do not support delivery. Please select PICKUP or remove those items.");
            }
        }

        Map<UUID, List<CartItem>> itemsByFarmer = cart.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getFarmer().getId()));

        String parentRef = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        List<OrderResponse> createdOrders = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : itemsByFarmer.entrySet()) {
            Order order = createOrderForFarmer(buyer, entry.getValue(), request, parentRef);
            createdOrders.add(orderMapper.toOrderResponse(order));
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Checkout completed for buyer {}. Created {} split orders under ref {}", buyer.getEmail(), createdOrders.size(), parentRef);
        return createdOrders;
    }

    // ==========================================
    // BUY NOW (Direct Checkout)
    // ==========================================
    @Transactional
    public OrderResponse buyNow(User buyer, BuyNowRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException("Product not found."));

        if (product.getFarmer().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot buy your own product.");
        }

        if (request.deliveryMethod() == DeliveryMethod.DELIVERY && !product.isDeliveryAvailable()) {
            throw new BusinessException("This product does not support delivery. Please select PICKUP.");
        }

        CartItem tempItem = CartItem.builder()
                .product(product)
                .quantity(request.quantity())
                .build();

        String parentRef = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Order order = createOrderForFarmer(buyer, List.of(tempItem),
                new OrderCreateRequest(request.deliveryMethod(), request.deliveryAddress(), request.buyerNotes()),
                parentRef);

        return orderMapper.toOrderResponse(order);
    }

    // ==========================================
    // STATE MACHINE (Status Updates)
    // ==========================================
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request, User currentUser) {
        // 1. Fetch Order (Lazy collections like getItems() will initialize safely within this @Transactional block)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 2. Strict Security Check
        if (!order.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: Only the assigned farmer can update this order.");
        }

        OrderStatus oldStatus = order.getStatus();

        // 3. Domain-Driven State Machine Validation
        // This throws a BusinessException if the transition is invalid (e.g., PENDING -> DELIVERED)
        order.transitionTo(request.newStatus());

        // 4. Update Latest Communication (Separation of current state vs. historical audit)
        if (request.notes() != null && !request.notes().isBlank()) {
            order.setFarmerNotes(request.notes());
        }

        // 5. Stock Management Logic (Strictly for Physical Goods)
        if (request.newStatus() == OrderStatus.ACCEPTED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product.getProductType() == ProductType.PHYSICAL_GOOD) {
                    updateProductStock(product, item.getApprovedQty(), false); // Deduct
                }
            }
            eventPublisher.publishEvent(new OrderAcceptedEvent(this, order));

        } else if (request.newStatus() == OrderStatus.CANCELLED || request.newStatus() == OrderStatus.REJECTED) {
            // Only restore stock if it was previously deducted (i.e., it was ACCEPTED or further along)
            if (oldStatus == OrderStatus.ACCEPTED ||
                    oldStatus == OrderStatus.PREPARING ||
                    oldStatus == OrderStatus.READY_FOR_PICKUP ||
                    oldStatus == OrderStatus.OUT_FOR_DELIVERY) {

                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    if (product.getProductType() == ProductType.PHYSICAL_GOOD) {
                        updateProductStock(product, item.getApprovedQty(), true); // Restore
                    }
                }
            }

            if (request.newStatus() == OrderStatus.REJECTED) {
                eventPublisher.publishEvent(new OrderRejectedEvent(this, order));
            }
        }

        // 6. Immutable Audit Trail
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(request.newStatus())
                .notes(request.notes())
                .build();
        historyRepository.save(history);

        // 7. Save and Handle Concurrency (Optimistic Locking)
        try {
            return orderMapper.toOrderResponse(orderRepository.save(order));
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure on order {} update by farmer {}", orderId, currentUser.getId());
            throw new BusinessException("Stock conflict detected. The product stock was updated by another user. Please refresh and try again.");
        }
    }

    // ==========================================
    // READ OPERATIONS
    // ==========================================
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getBuyer().getId().equals(currentUser.getId()) && !order.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: You do not have permission to view this order.");
        }

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Order getOrderEntityById(UUID orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getBuyer().getId().equals(currentUser.getId()) && !order.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: You do not have permission to view this order.");
        }
        return order;
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    // Helper to extract BigDecimal from JSONB
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

    // Helper to update JSONB stock safely
    private void updateProductStock(Product product, BigDecimal qty, boolean restore) {
        BigDecimal currentStock = getAttributeAsDecimal(product, "availableStock");
        if (currentStock != null) {
            BigDecimal newStock = restore ? currentStock.add(qty) : currentStock.subtract(qty);
            // Jackson's JsonNode is immutable, but Hibernate maps it as ObjectNode which is mutable
            if (product.getAttributes() instanceof ObjectNode objNode) {
                objNode.put("availableStock", newStock);
                product.setAttributes(objNode);
            }
        }
    }

    private Order createOrderForFarmer(User buyer, List<CartItem> items, OrderCreateRequest request, String parentRef) {
        User farmer = items.get(0).getProduct().getFarmer();

        Order order = Order.builder()
                .orderNumber(parentRef + "-" + farmer.getId().toString().substring(0, 4).toUpperCase())
                .parentOrderRef(parentRef)
                .buyer(buyer)
                .farmer(farmer)
                .status(OrderStatus.PENDING)
                .deliveryMethod(request.deliveryMethod())
                .deliveryAddress(request.deliveryAddress())
                .buyerNotes(request.buyerNotes())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : items) {
            Product freshProduct = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException("Product no longer exists."));

            // ONLY check stock for physical goods
            if (freshProduct.getProductType() == ProductType.PHYSICAL_GOOD) {
                BigDecimal availableStock = getAttributeAsDecimal(freshProduct, "availableStock");
                if (availableStock != null && availableStock.compareTo(cartItem.getQuantity()) < 0) {
                    throw new BusinessException("Insufficient stock for " + freshProduct.getTitle() + ".");
                }
            }

            BigDecimal subtotal = freshProduct.getPrice().multiply(cartItem.getQuantity());

            if (request.deliveryMethod() == DeliveryMethod.DELIVERY && freshProduct.isDeliveryAvailable() && freshProduct.getDeliveryFee() != null) {
                subtotal = subtotal.add(freshProduct.getDeliveryFee());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(freshProduct)
                    .productTitleSnapshot(freshProduct.getTitle())
                    .unitPriceSnapshot(freshProduct.getPrice())
                    .attributesSnapshot(freshProduct.getAttributes()) // SNAPSHOT THE JSONB!
                    .requestedQty(cartItem.getQuantity())
                    .approvedQty(cartItem.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    // ==========================================
    // ORDER HISTORY (READ OPERATIONS)
    // ==========================================

    @Transactional(readOnly = true)
    public Page<OrderResponse> getBuyerOrders(User buyer, String status, Pageable pageable) {
        Page<Order> orders;

        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyer.getId(), orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid order status provided.");
            }
        } else {
            orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId(), pageable);
        }

        return orders.map(orderMapper::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getFarmerOrders(User farmer, String status, Pageable pageable) {
        Page<Order> orders;

        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByFarmerIdAndStatusOrderByCreatedAtDesc(farmer.getId(), orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid order status provided.");
            }
        } else {
            orders = orderRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId(), pageable);
        }

        return orders.map(orderMapper::toOrderResponse);
    }

    // Allow Buyer to mark order as completed (unlocks Reviews)
    @Transactional
    public OrderResponse confirmReceipt(UUID orderId, User buyer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException("Access Denied: You can only confirm your own orders.");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("You can only mark an order as completed after it has been delivered.");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.COMPLETED);

        // Optional: Save to history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(OrderStatus.COMPLETED)
                .notes("Buyer confirmed receipt")
                .build();
        historyRepository.save(history);

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }
}