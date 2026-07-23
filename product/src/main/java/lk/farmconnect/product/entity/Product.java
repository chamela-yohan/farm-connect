package lk.farmconnect.product.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lk.farmconnect.user.User;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
@SQLRestriction("is_deleted = false")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    // Common Trading Rules
    @Column(name = "min_order_qty", precision = 10, scale = 2)
    private BigDecimal minOrderQty;

    @Column(name = "max_order_qty", precision = 10, scale = 2)
    private BigDecimal maxOrderQty;

    @Column(name = "qty_step", precision = 10, scale = 2)
    private BigDecimal qtyStep;

    // Delivery
    @Column(name = "is_delivery_available")
    private boolean isDeliveryAvailable = false;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    // JSONB Attributes (Stock, Unit, Expiry, Rental Rules, etc.)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode attributes;

    // Media
    @Column(name = "video_url")
    private String videoUrl;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // Multiple Locations (References the City table)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProductLocation> locations = new HashSet<>();

    // Delivery Areas (Set of District IDs)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_delivery_areas", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "district_id")
    @Builder.Default
    private Set<Integer> deliveryDistrictIds = new HashSet<>();

    // Metadata & Optimistic Locking
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    public void softDelete() {
        this.isDeleted = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}