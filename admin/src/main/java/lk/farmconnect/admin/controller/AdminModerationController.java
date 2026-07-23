package lk.farmconnect.admin.controller;

import lk.farmconnect.admin.service.AdminModerationService;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.review.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationService moderationService;

    // --- PRODUCTS ---

    @GetMapping("/products/deleted")
    public ResponseEntity<ApiResponse<Page<Product>>> getDeletedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(moderationService.getDeletedProducts(pageable)));
    }

    @PutMapping("/products/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hideProduct(@PathVariable UUID id) {
        moderationService.moderateProduct(id, true);
        return ResponseEntity.ok(ApiResponse.success(null, "Product hidden successfully"));
    }

    @PutMapping("/products/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreProduct(@PathVariable UUID id) {
        moderationService.moderateProduct(id, false);
        return ResponseEntity.ok(ApiResponse.success(null, "Product restored successfully"));
    }

    // --- REVIEWS ---

    @GetMapping("/reviews/deleted")
    public ResponseEntity<ApiResponse<Page<Review>>> getDeletedReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(moderationService.getDeletedReviews(pageable)));
    }

    @PutMapping("/reviews/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hideReview(@PathVariable UUID id) {
        moderationService.moderateReview(id, true);
        return ResponseEntity.ok(ApiResponse.success(null, "Review hidden successfully"));
    }

    @PutMapping("/reviews/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreReview(@PathVariable UUID id) {
        moderationService.moderateReview(id, false);
        return ResponseEntity.ok(ApiResponse.success(null, "Review restored successfully"));
    }
}