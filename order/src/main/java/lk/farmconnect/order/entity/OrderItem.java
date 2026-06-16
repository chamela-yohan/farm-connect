package lk.farmconnect.order.entity;

import jakarta.persistence.*;
import lk.farmconnect.product.entity.Product;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Reference to product (for UI links), but we NEVER use product.getPrice() for billing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // CRITICAL SNAPSHOTS: Protects historical data from future price/title changes
    @Column(nullable = false)
    private String productTitleSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    // NEGOTIATION MODEL: What buyer wanted vs what farmer approved
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQty;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal approvedQty; // Initially equals requestedQty

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal; // approvedQty * unitPriceSnapshot
}