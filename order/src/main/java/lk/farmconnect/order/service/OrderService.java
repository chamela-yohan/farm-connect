package lk.farmconnect.order.service;

import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.order.dto.*;
import lk.farmconnect.order.entity.*;
import lk.farmconnect.order.mapper.OrderMapper;
import lk.farmconnect.order.repository.CartRepository;
import lk.farmconnect.order.repository.OrderRepository;
import lk.farmconnect.order.repository.OrderStatusHistoryRepository;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
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

        // VALIDATE DELIVERY CAPABILITY ACROSS ALL ITEMS
        if (request.deliveryMethod() == DeliveryMethod.DELIVERY) {
            boolean allSupportDelivery = cart.getItems().stream()
                    .allMatch(item -> item.getProduct().isDeliveryAvailable());
            if (!allSupportDelivery) {
                throw new BusinessException("Some items in your cart do not support delivery. Please select PICKUP or remove those items.");
            }
        }

        // GROUP ITEMS BY FARMER (Order Splitting Logic)
        Map<UUID, List<CartItem>> itemsByFarmer = cart.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getFarmer().getId()));

        // Generate a shared reference for this checkout session
        String parentRef = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        List<OrderResponse> createdOrders = new ArrayList<>();

        // CREATE A SEPARATE ORDER FOR EACH FARMER
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByFarmer.entrySet()) {
            Order order = createOrderForFarmer(buyer, entry.getValue(), request, parentRef);
            createdOrders.add(orderMapper.toOrderResponse(order));
        }

        // Clear Cart
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

        // Prevent self-purchase
        if (product.getFarmer().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot buy your own product.");
        }

        // Validate Delivery Capability
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: Only the assigned farmer can update this order.");
        }

        OrderStatus oldStatus = order.getStatus();
        order.transitionTo(request.newStatus());

        // STOCK MANAGEMENT LOGIC
        if (request.newStatus() == OrderStatus.ACCEPTED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setAvailableStock(product.getAvailableStock().subtract(item.getApprovedQty()));
            }
        } else if (request.newStatus() == OrderStatus.CANCELLED || request.newStatus() == OrderStatus.REJECTED) {
            if (oldStatus == OrderStatus.ACCEPTED || oldStatus == OrderStatus.READY_FOR_PICKUP || oldStatus == OrderStatus.OUT_FOR_DELIVERY) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    product.setAvailableStock(product.getAvailableStock().add(item.getApprovedQty()));
                }
            }
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(request.newStatus())
                .notes(request.notes())
                .build();
        historyRepository.save(history);

        try {
            return orderMapper.toOrderResponse(orderRepository.save(order));
        } catch (OptimisticLockingFailureException e) {
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

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================
    private Order createOrderForFarmer(User buyer, List<CartItem> items, OrderCreateRequest request, String parentRef) {
        User farmer = items.get(0).getProduct().getFarmer();

        Order order = Order.builder()
                .orderNumber(parentRef + "-" + farmer.getId().toString().substring(0, 4).toUpperCase()) // Unique per farmer
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

            if (freshProduct.getAvailableStock().compareTo(cartItem.getQuantity()) < 0) {
                throw new BusinessException("Insufficient stock for " + freshProduct.getTitle() + ".");
            }

            BigDecimal subtotal = freshProduct.getPrice().multiply(cartItem.getQuantity());

            // ADD DELIVERY FEE IF APPLICABLE
            if (request.deliveryMethod() == DeliveryMethod.DELIVERY && freshProduct.getDeliveryFee() != null) {
                subtotal = subtotal.add(freshProduct.getDeliveryFee());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(freshProduct)
                    .productTitleSnapshot(freshProduct.getTitle())
                    .unitPriceSnapshot(freshProduct.getPrice())
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
}