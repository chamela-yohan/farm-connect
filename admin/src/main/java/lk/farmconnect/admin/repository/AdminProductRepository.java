package lk.farmconnect.admin.repository;

import lk.farmconnect.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminProductRepository extends JpaRepository<Product, UUID> {

    // Bypass @SQLRestriction to find a specific product (even if deleted)
    @Query(nativeQuery = true, value = "SELECT * FROM products WHERE id = :id")
    Optional<Product> findByIdIncludingDeleted(@Param("id") UUID id);

    // Bypass @SQLRestriction to list ALL deleted products
    @Query(nativeQuery = true, value = "SELECT * FROM products WHERE is_deleted = true ORDER BY updated_at DESC")
    Page<Product> findAllDeleted(Pageable pageable);

    // Native update to flip the boolean (Bypasses entity lifecycle for performance)
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE products SET is_deleted = :isDeleted, updated_at = NOW() WHERE id = :id")
    void updateDeletedStatus(@Param("id") UUID id, @Param("isDeleted") boolean isDeleted);
}