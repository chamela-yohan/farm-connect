package lk.farmconnect.review.controller;

import jakarta.validation.Valid;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.review.dto.ReviewCreateRequest;
import lk.farmconnect.review.dto.ReviewResponse;
import lk.farmconnect.review.service.ReviewService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // BUYER ENDPOINT: Submit a review
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.createReview(buyer, request)));
    }

    // PUBLIC ENDPOINT: View a farmer's reviews
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getFarmerReviews(
            @PathVariable UUID farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(reviewService.getFarmerReviews(farmerId, pageable)));
    }
}