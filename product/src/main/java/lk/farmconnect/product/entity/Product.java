package lk.farmconnect.product.entity;

import jakarta.persistence.*;
import lk.farmconnect.user.User;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes; // e.g., {"weight_kg": 5, "expiry_date": "2026-07-01"}

    @Version // Optimistic looking; Prevents race conditions when two buyers buy the last stock
    private Integer version;

    @Column(precision = 10, scale = 2) // Actual trading rules
    private BigDecimal availableStock; // Current live stock

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderQty; // e.g., 1.0 kg

    @Column(precision = 10, scale = 2)
    private BigDecimal maxOrderQty; // e.g., 10.0 kg

    @Column(precision = 10, scale = 2)
    private BigDecimal qtyStep;        // e.g., 0.5 kg increments

    // DELIVERY CAPABILITY
    @Column(nullable = false)
    private boolean isDeliveryAvailable; // True if farmer can deliver, False for pickup only

    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee; // Fee charged if buyer selects DELIVERY

    private LocalDate expiryDate; // Null for non-perishables (like tractors)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", nullable = false)
    private List<String> imageUrls;

    // Optional video
    @Column(name = "video_url")
    private String videoUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ProductStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}