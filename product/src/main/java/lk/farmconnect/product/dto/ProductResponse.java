package lk.farmconnect.product.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lk.farmconnect.product.entity.ProductStatus;
import lk.farmconnect.product.entity.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        ProductType productType,
        ProductStatus status,

        BigDecimal minOrderQty,
        BigDecimal maxOrderQty,
        BigDecimal qtyStep,

        boolean isDeliveryAvailable,
        BigDecimal deliveryFee,

        UUID categoryId,
        String categoryName,

        JsonNode attributes,

        List<String> imageUrls, // Presigned URLs
        String videoUrl,        // Presigned URL

        List<LocationDetail> locations,
        Set<Integer> deliveryDistrictIds,

        UUID farmerId,
        String farmerName,

        Integer version, // For optimistic locking
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record LocationDetail(
            Integer cityId,
            String cityName,
            Integer districtId,
            String districtName // Can be resolved by frontend using the 25 districts list
    ) {}
}