package lk.farmconnect.review.repository;

import lk.farmconnect.order.entity.Order;
import lk.farmconnect.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Check if a review already exists for this order
    boolean existsByOrder(Order order);

    // Fetch reviews for a specific farmer (for their public profile)
    Page<Review> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId, Pageable pageable);
}