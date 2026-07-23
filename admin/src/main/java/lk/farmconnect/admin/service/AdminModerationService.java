package lk.farmconnect.admin.service;

import lk.farmconnect.common.event.AdminAuditEvent;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.admin.repository.AdminProductRepository;
import lk.farmconnect.review.entity.Review;
import lk.farmconnect.admin.repository.AdminReviewRepository;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private final AdminProductRepository adminProductRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ==========================================
    // PRODUCT MODERATION
    // ==========================================

    @Transactional
    public void moderateProduct(UUID productId, boolean shouldDelete) {
        Product product = adminProductRepository.findByIdIncludingDeleted(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Prevent redundant updates
        if (product.isDeleted() == shouldDelete) return;

        adminProductRepository.updateDeletedStatus(productId, shouldDelete);

        // Publish Audit Event
        User admin = getCurrentAdmin();
        eventPublisher.publishEvent(new AdminAuditEvent(
                this,
                admin.getId(),
                shouldDelete ? "SOFT_DELETE_PRODUCT" : "RESTORE_PRODUCT",
                productId.toString(),
                "Product '" + product.getTitle() + "' was " + (shouldDelete ? "hidden" : "restored") + " by admin."
        ));
    }

    @Transactional(readOnly = true)
    public Page<Product> getDeletedProducts(Pageable pageable) {
        return adminProductRepository.findAllDeleted(pageable);
    }

    // ==========================================
    // REVIEW MODERATION
    // ==========================================

    @Transactional
    public void moderateReview(UUID reviewId, boolean shouldDelete) {
        Review review = adminReviewRepository.findByIdIncludingDeleted(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (review.isDeleted() == shouldDelete) return;

        adminReviewRepository.updateDeletedStatus(reviewId, shouldDelete);

        User admin = getCurrentAdmin();
        eventPublisher.publishEvent(new AdminAuditEvent(
                this,
                admin.getId(),
                shouldDelete ? "SOFT_DELETE_REVIEW" : "RESTORE_REVIEW",
                reviewId.toString(),
                "Review by '" + review.getBuyer().getName() + "' was " + (shouldDelete ? "hidden" : "restored") + " by admin."
        ));
    }

    @Transactional(readOnly = true)
    public Page<Review> getDeletedReviews(Pageable pageable) {
        return adminReviewRepository.findAllDeleted(pageable);
    }

    // Helper
    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new RuntimeException("Authentication required");
        }
        return (User) auth.getPrincipal();
    }
}