package lk.farmconnect.order.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lk.farmconnect.product.entity.Product;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String productTitleSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    // NEW: Snapshot the JSONB attributes at the time of purchase
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode attributesSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQty;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal approvedQty;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}