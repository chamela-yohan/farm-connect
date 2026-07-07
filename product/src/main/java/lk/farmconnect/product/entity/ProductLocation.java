package lk.farmconnect.product.entity;

import jakarta.persistence.*;
import lk.farmconnect.common.entity.City; // From your common module
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "product_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // EAGER fetch because we need city names for search results and frontend display
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;
}