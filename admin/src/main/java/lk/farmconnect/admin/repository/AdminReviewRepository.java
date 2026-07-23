package lk.farmconnect.admin.repository;

import lk.farmconnect.review.entity.Review;
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
public interface AdminReviewRepository extends JpaRepository<Review, UUID> {

    @Query(nativeQuery = true, value = "SELECT * FROM reviews WHERE id = :id")
    Optional<Review> findByIdIncludingDeleted(@Param("id") UUID id);

    @Query(nativeQuery = true, value = "SELECT * FROM reviews WHERE is_deleted = true ORDER BY created_at DESC")
    Page<Review> findAllDeleted(Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE reviews SET is_deleted = :isDeleted WHERE id = :id")
    void updateDeletedStatus(@Param("id") UUID id, @Param("isDeleted") boolean isDeleted);
}