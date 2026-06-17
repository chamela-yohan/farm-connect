package lk.farmconnect.order.service;

import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.order.dto.OrderCreateRequest;
import lk.farmconnect.order.dto.OrderResponse;
import lk.farmconnect.order.dto.OrderStatusUpdateRequest;
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
import java.util.UUID;

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
    // CHECKOUT (Cart -> Order)
    // ==========================================
    @Transactional
    public OrderResponse checkout(User buyer, OrderCreateRequest request) {
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new BusinessException("Cart is empty or not found."));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cannot checkout with an empty cart.");
        }

        // V1 BUSINESS RULE: Single-Farmer Cart
        UUID farmerId = cart.getItems().get(0).getProduct().getFarmer().getId();
        boolean hasMultipleFarmers = cart.getItems().stream()
                .anyMatch(item -> !item.getProduct().getFarmer().getId().equals(farmerId));

        if (hasMultipleFarmers) {
            throw new BusinessException("V1 Checkout Error: Cart contains items from multiple farmers. Please checkout separately.");
        }

        // Create Order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .buyer(buyer)
                .farmer(cart.getItems().get(0).getProduct().getFarmer()) // Denormalized
                .status(OrderStatus.PENDING)
                .deliveryMethod(request.deliveryMethod())
                .deliveryAddress(request.deliveryAddress())
                .buyerNotes(request.buyerNotes())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            // Fetch fresh product to ensure stock hasn't changed while in cart
            Product freshProduct = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException("Product no longer exists."));

            if (freshProduct.getAvailableStock().compareTo(cartItem.getQuantity()) < 0) {
                throw new BusinessException("Insufficient stock for " + freshProduct.getTitle() + ". Please update your cart.");
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(freshProduct)
                    .productTitleSnapshot(freshProduct.getTitle())
                    .unitPriceSnapshot(freshProduct.getPrice())
                    .requestedQty(cartItem.getQuantity())
                    .approvedQty(cartItem.getQuantity()) // Initially same
                    .subtotal(freshProduct.getPrice().multiply(cartItem.getQuantity()))
                    .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Clear Cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order {} created by buyer {} for farmer {}", savedOrder.getOrderNumber(), buyer.getEmail(), order.getFarmer().getEmail());
        return orderMapper.toOrderResponse(savedOrder);
    }

    // ==========================================
    // STATE MACHINE (Status Updates & Stock Logic)
    // ==========================================
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // SECURITY: Only the farmer who owns the order can update its status
        if (!order.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: Only the assigned farmer can update this order.");
        }

        OrderStatus oldStatus = order.getStatus();

        // Domain-Driven Validation (Throws exception if invalid)
        order.transitionTo(request.newStatus());

        // STOCK MANAGEMENT LOGIC
        if (request.newStatus() == OrderStatus.ACCEPTED) {
            // Deduct stock based on approvedQty
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setAvailableStock(product.getAvailableStock().subtract(item.getApprovedQty()));
            }
        } else if (request.newStatus() == OrderStatus.CANCELLED || request.newStatus() == OrderStatus.REJECTED) {
            // Restore stock if it was previously accepted (and thus deducted)
            if (oldStatus == OrderStatus.ACCEPTED || oldStatus == OrderStatus.READY_FOR_PICKUP || oldStatus == OrderStatus.OUT_FOR_DELIVERY) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    product.setAvailableStock(product.getAvailableStock().add(item.getApprovedQty()));
                }
            }
        }

        // Save Audit Trail
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

        // Security check
        if (!order.getBuyer().getId().equals(currentUser.getId()) && !order.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: You do not have permission to view this order.");
        }

        return orderMapper.toOrderResponse(order);
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}