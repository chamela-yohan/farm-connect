package lk.farmconnect.product.repository;

import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.entity.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByIsDeletedFalse(Pageable pageable);

    Page<Product> findByFarmerIdAndIsDeletedFalse(UUID farmerId, Pageable pageable);

    Optional<Product> findByIdAndIsDeletedFalse(UUID id);

    @Query(value = "SELECT DISTINCT attributes->>'category' as category FROM products WHERE is_deleted = false AND status = 'ACTIVE' AND attributes->>'category' IS NOT NULL ORDER BY category", nativeQuery = true)
    List<String> findDistinctCategories();

    @Query(value = """
        SELECT DISTINCT p.* FROM products p
        LEFT JOIN product_locations pl ON p.id = pl.product_id
        LEFT JOIN cities c ON pl.city_id = c.id
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        
        -- 1. Basic Filters
        AND (:keyword IS NULL OR to_tsvector('english', p.title || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :keyword))
        AND (:categoryId IS NULL OR p.category_id = :categoryId)
        AND (:productType IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        
        -- 2. Location Filters
        AND (:locationDistrictId IS NULL OR c.district_id = :locationDistrictId)
        AND (:lat IS NULL OR :lon IS NULL OR ST_DWithin(c.coordinates::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radiusKm * 1000))
        
        -- 3. Delivery Filters
        AND (:isDeliveryAvailable IS NULL OR p.is_delivery_available = :isDeliveryAvailable)
        AND (:deliveryDistrictId IS NULL OR EXISTS (
            SELECT 1 FROM product_delivery_areas pda
            WHERE pda.product_id = p.id AND pda.district_id = :deliveryDistrictId
        ))
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p.id) FROM products p
        LEFT JOIN product_locations pl ON p.id = pl.product_id
        LEFT JOIN cities c ON pl.city_id = c.id
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (:keyword IS NULL OR to_tsvector('english', p.title || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :keyword))
        AND (:categoryId IS NULL OR p.category_id = :categoryId)
        AND (:productType IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:locationDistrictId IS NULL OR c.district_id = :locationDistrictId)
        AND (:lat IS NULL OR :lon IS NULL OR ST_DWithin(c.coordinates::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radiusKm * 1000))
        AND (:isDeliveryAvailable IS NULL OR p.is_delivery_available = :isDeliveryAvailable)
        AND (:deliveryDistrictId IS NULL OR EXISTS (SELECT 1 FROM product_delivery_areas pda WHERE pda.product_id = p.id AND pda.district_id = :deliveryDistrictId))
        """,
            nativeQuery = true)
    Page<Product> searchProductsAdvanced(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("productType") String productType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("locationDistrictId") Integer locationDistrictId,
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("radiusKm") Double radiusKm,
            @Param("isDeliveryAvailable") Boolean isDeliveryAvailable,
            @Param("deliveryDistrictId") Integer deliveryDistrictId,
            Pageable pageable
    );
    @Query(value = "SELECT DISTINCT title FROM products WHERE title ILIKE CONCAT('%', :query, '%') AND is_deleted = false AND status = 'ACTIVE' LIMIT 5", nativeQuery = true)
    List<String> findKeywordSuggestions(@Param("query") String query);
}