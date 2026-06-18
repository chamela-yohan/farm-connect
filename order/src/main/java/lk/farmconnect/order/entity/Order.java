package lk.farmconnect.order.entity;

import jakarta.persistence.*;
import lk.farmconnect.user.User;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_buyer_status", columnList = "buyer_id, status"),
        @Index(name = "idx_order_farmer_status", columnList = "farmer_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String orderNumber; // e.g., "ORD-2026-001"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    // Denormalized for performance (Farmer Dashboard optimization)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String buyerNotes;

    @Column(columnDefinition = "TEXT")
    private String farmerNotes;

    @Column(updatable = false)
    private String parentOrderRef; // e.g., "ORD-2026-001" (Shared across split orders)

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    //Domain-Driven state machine
    public void transitionTo(OrderStatus newStatus) {
        if (!getAllowedTransitions(this.status).contains(newStatus)) {
            throw new lk.farmconnect.common.exception.BusinessException(
                    "Invalid status transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
    }

    private java.util.List<OrderStatus> getAllowedTransitions(OrderStatus current) {
        return
                switch (current) {
                    case PENDING ->
                            java.util.List.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.CANCELLED);
                    case ACCEPTED ->
                            java.util.List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED);
                    case READY_FOR_PICKUP ->
                            java.util.List.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.CANCELLED);
                    case OUT_FOR_DELIVERY -> java.util.List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
                    case DELIVERED -> java.util.List.of(OrderStatus.COMPLETED);
                    // Terminal states cannot transition
                    case REJECTED, CANCELLED, COMPLETED -> java.util.List.of();
                };
    }

}