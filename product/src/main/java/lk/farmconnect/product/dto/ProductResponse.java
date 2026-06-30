package lk.farmconnect.product.dto;

import lk.farmconnect.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String title,
        String description,
        Integer version,
        BigDecimal availableStock,
        BigDecimal minOrderQty,
        BigDecimal maxOrderQty,
        BigDecimal qtyStep,
        boolean isDeliveryAvailable,
        BigDecimal deliveryFee,
        LocalDate expiryDate,
        BigDecimal price,
        List<String> imageUrls,
        String videoUrl,
        Map<String, Object> attributes,
        ProductStatus status,
        UUID farmerId,
        String farmerName,
        LocalDateTime createdAt,
        Double lat,
        Double lon
) {}