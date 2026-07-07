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

    // Search with location - ALL parameters have explicit type casts
    @Query(value = """
        SELECT p.* FROM products p
        JOIN users f ON p.farmer_id = f.id
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (CAST(:keyword AS VARCHAR) IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')) 
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')))
        AND (CAST(:category AS VARCHAR) IS NULL OR p.attributes->>'category' = CAST(:category AS VARCHAR))
        AND (CAST(:productType AS VARCHAR) IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (CAST(:minPrice AS DECIMAL) IS NULL OR p.price >= CAST(:minPrice AS DECIMAL))
        AND (CAST(:maxPrice AS DECIMAL) IS NULL OR p.price <= CAST(:maxPrice AS DECIMAL))
        AND (CAST(:isDeliveryAvailable AS BOOLEAN) IS NULL OR p.is_delivery_available = CAST(:isDeliveryAvailable AS BOOLEAN))
        AND (
            f.location IS NULL 
            OR ST_DWithin(
                f.location::geography,
                ST_SetSRID(ST_MakePoint(CAST(:lon AS DECIMAL), CAST(:lat AS DECIMAL)), 4326)::geography,
                CAST(:radiusKm AS DECIMAL) * 1000
            )
        )
        """,
            countQuery = """
        SELECT COUNT(*) FROM products p
        JOIN users f ON p.farmer_id = f.id
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (CAST(:keyword AS VARCHAR) IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')) 
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')))
        AND (CAST(:category AS VARCHAR) IS NULL OR p.attributes->>'category' = CAST(:category AS VARCHAR))
        AND (CAST(:productType AS VARCHAR) IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (CAST(:minPrice AS DECIMAL) IS NULL OR p.price >= CAST(:minPrice AS DECIMAL))
        AND (CAST(:maxPrice AS DECIMAL) IS NULL OR p.price <= CAST(:maxPrice AS DECIMAL))
        AND (CAST(:isDeliveryAvailable AS BOOLEAN) IS NULL OR p.is_delivery_available = CAST(:isDeliveryAvailable AS BOOLEAN))
        AND (
            f.location IS NULL 
            OR ST_DWithin(
                f.location::geography,
                ST_SetSRID(ST_MakePoint(CAST(:lon AS DECIMAL), CAST(:lat AS DECIMAL)), 4326)::geography,
                CAST(:radiusKm AS DECIMAL) * 1000
            )
        )
        """,
            nativeQuery = true)
    Page<Product> searchWithLocation(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("productType") ProductType productType,
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("radiusKm") Double radiusKm,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("isDeliveryAvailable") Boolean isDeliveryAvailable,
            Pageable pageable
    );

    // FIXED: Search without location - ALL parameters have explicit type casts
    @Query(value = """
        SELECT p.* FROM products p
        JOIN users f ON p.farmer_id = f.id
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (CAST(:keyword AS VARCHAR) IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')) 
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')))
        AND (CAST(:category AS VARCHAR) IS NULL OR p.attributes->>'category' = CAST(:category AS VARCHAR))
        AND (CAST(:productType AS VARCHAR) IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (CAST(:minPrice AS DECIMAL) IS NULL OR p.price >= CAST(:minPrice AS DECIMAL))
        AND (CAST(:maxPrice AS DECIMAL) IS NULL OR p.price <= CAST(:maxPrice AS DECIMAL))
        AND (CAST(:isDeliveryAvailable AS BOOLEAN) IS NULL OR p.is_delivery_available = CAST(:isDeliveryAvailable AS BOOLEAN))
        """,
            countQuery = """
        SELECT COUNT(*) FROM products p
        JOIN users f ON p.farmer_id = f.id
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (CAST(:keyword AS VARCHAR) IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')) 
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS VARCHAR), '%')))
        AND (CAST(:category AS VARCHAR) IS NULL OR p.attributes->>'category' = CAST(:category AS VARCHAR))
        AND (CAST(:productType AS VARCHAR) IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (CAST(:minPrice AS DECIMAL) IS NULL OR p.price >= CAST(:minPrice AS DECIMAL))
        AND (CAST(:maxPrice AS DECIMAL) IS NULL OR p.price <= CAST(:maxPrice AS DECIMAL))
        AND (CAST(:isDeliveryAvailable AS BOOLEAN) IS NULL OR p.is_delivery_available = CAST(:isDeliveryAvailable AS BOOLEAN))
        """,
            nativeQuery = true)
    Page<Product> searchWithoutLocation(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("productType") ProductType productType,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("isDeliveryAvailable") Boolean isDeliveryAvailable,
            Pageable pageable
    );

    // NEW: SEARCH FEATURE
    @Query(value = """
        SELECT p.* FROM products p
        LEFT JOIN product_locations pl ON p.id = pl.product_id AND pl.is_primary = true
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (:keyword IS NULL OR to_tsvector('english', p.title || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :keyword))
        AND (:productType IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (:categoryId IS NULL OR p.category_id = :categoryId)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:minStock IS NULL OR (p.attributes->>'availableStock')::numeric >= :minStock)
        AND (:minRentalDays IS NULL OR (p.attributes->>'minRental')::integer >= :minRentalDays)
        AND (:maxRentalDays IS NULL OR (p.attributes->>'maxRental')::integer <= :maxRentalDays)
        AND (:deliveryAvailable IS NULL OR p.is_delivery_available = :deliveryAvailable)
        AND (:city IS NULL OR LOWER(pl.city) = LOWER(:city))
        AND (:latitude IS NULL OR :longitude IS NULL OR 
             ST_DWithin(pl.coordinates::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusKm * 1000))
        """,
            countQuery = """
        SELECT COUNT(p.id) FROM products p
        LEFT JOIN product_locations pl ON p.id = pl.product_id AND pl.is_primary = true
        WHERE p.is_deleted = false
        AND p.status = 'ACTIVE'
        AND (:keyword IS NULL OR to_tsvector('english', p.title || ' ' || COALESCE(p.description, '')) @@ plainto_tsquery('english', :keyword))
        AND (:productType IS NULL OR p.product_type = CAST(:productType AS VARCHAR))
        AND (:category IS NULL OR p.attributes->>'category' = :category)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:minStock IS NULL OR (p.attributes->>'availableStock')::numeric >= :minStock)
        AND (:minRentalDays IS NULL OR (p.attributes->>'minRental')::integer >= :minRentalDays)
        AND (:maxRentalDays IS NULL OR (p.attributes->>'maxRental')::integer <= :maxRentalDays)
        AND (:deliveryAvailable IS NULL OR p.is_delivery_available = :deliveryAvailable)
        AND (:city IS NULL OR LOWER(pl.city) = LOWER(:city))
        AND (:latitude IS NULL OR :longitude IS NULL OR 
             ST_DWithin(pl.coordinates::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusKm * 1000))
        """,
            nativeQuery = true)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("productType") ProductType productType,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("city") String city,
            @Param("minStock") BigDecimal minStock,
            @Param("minRentalDays") Integer minRentalDays,
            @Param("maxRentalDays") Integer maxRentalDays,
            @Param("deliveryAvailable") Boolean deliveryAvailable,
            Pageable pageable
    );
}