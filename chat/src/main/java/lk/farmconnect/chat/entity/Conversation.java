package lk.farmconnect.chat.entity;

import jakarta.persistence.*;
import lk.farmconnect.booking.entity.Booking;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.user.User;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_order", columnList = "order_id", unique = true),
        @Index(name = "idx_conv_booking", columnList = "booking_id", unique = true),
        @Index(name = "idx_conv_users", columnList = "buyer_id, farmer_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id",nullable = true, unique = true)
    private Order order;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id",nullable = true, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}