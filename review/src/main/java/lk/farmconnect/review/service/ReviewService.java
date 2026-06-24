package lk.farmconnect.review.service;

import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.entity.OrderStatus;
import lk.farmconnect.order.repository.OrderRepository;
import lk.farmconnect.review.dto.ReviewCreateRequest;
import lk.farmconnect.review.dto.ReviewResponse;
import lk.farmconnect.review.entity.Review;
import lk.farmconnect.review.mapper.ReviewMapper;
import lk.farmconnect.review.repository.ReviewRepository;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(User buyer, ReviewCreateRequest request) {
        // Fetch Order
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Validate Ownership
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException("Access Denied: You can only review your own orders.");
        }

        // Validate Order Status (Must be COMPLETED)
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("You can only review orders that are marked as COMPLETED.");
        }

        // Prevent Duplicate Reviews
        if (reviewRepository.existsByOrder(order)) {
            throw new BusinessException("You have already reviewed this order.");
        }

        // Create and Save Review
        Review review = Review.builder()
                .order(order)
                .buyer(buyer)
                .farmer(order.getFarmer())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Update Farmer's Aggregate Rating (O(1) Math)
        updateFarmerRatingStats(order.getFarmer(), request.rating());

        log.info("Review created for order {} by buyer {}", order.getOrderNumber(), buyer.getEmail());
        return reviewMapper.toResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getFarmerReviews(UUID farmerId, Pageable pageable) {
        return reviewRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId, pageable)
                .map(reviewMapper::toResponse);
    }

    // O(1) Mathematical Update: No need to query all reviews!
    private void updateFarmerRatingStats(User farmer, int newRating) {
        int currentTotal = farmer.getTotalReviews() != null ? farmer.getTotalReviews() : 0;
        double currentAvg = farmer.getAverageRating() != null ? farmer.getAverageRating() : 0.0;

        // Formula: New Average = ((Old Average * Total Count) + New Rating) / (Total Count + 1)
        double newAverage = ((currentAvg * currentTotal) + newRating) / (currentTotal + 1);

        farmer.setTotalReviews(currentTotal + 1);
        farmer.setAverageRating(Math.round(newAverage * 10.0) / 10.0); // Round to 1 decimal place

        userRepository.save(farmer);
    }
}