package lk.farmconnect.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID buyerId,
        String buyerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}