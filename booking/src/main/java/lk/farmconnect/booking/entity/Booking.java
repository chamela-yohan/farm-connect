package lk.farmconnect.booking.entity;

import jakarta.persistence.*;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.user.User;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer totalDays;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rentalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount; // rentalAmount + depositAmount

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private String buyerNotes;
    private String farmerNotes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = BookingStatus.PENDING;
    }
}