package lk.farmconnect.product.dto;

import lk.farmconnect.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        Map<String, Object> attributes,
        ProductStatus status,
        UUID farmerId,
        String farmerName,
        LocalDateTime createdAt
) {}