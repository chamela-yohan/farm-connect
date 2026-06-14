package lk.farmconnect.product.repository;

import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.entity.ProductStatus;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    // ==========================================
    // STANDARD QUERIES (With N+1 Prevention)
    // ==========================================

    // @EntityGraph forces a single LEFT JOIN query to fetch the farmer's details
    @EntityGraph(attributePaths = {"farmer"})
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"farmer"})
    Optional<Product> findByIdAndIsDeletedFalse(UUID id);

    // ==========================================
    // GEO-SPATIAL SEARCH (PostGIS)
    // ==========================================

    /*
     * We pass the JTS 'Point' object directly from the Service layer.
     *
     * Note: ST_DWithin expects the radius in METERS when using the 'geography' type.
     */
    @EntityGraph(attributePaths = {"farmer"})
    @Query("SELECT p FROM Product p JOIN p.farmer f WHERE " +
            "p.isDeleted = false AND p.status = :status AND " +
            "ST_DWithin(f.location, :buyerLocation, :radiusInMeters) = true")
    Page<Product> findNearbyActiveProducts(
            @Param("buyerLocation") Point buyerLocation,
            @Param("radiusInMeters") double radiusInMeters,
            @Param("status") ProductStatus status,
            Pageable pageable
    );
}