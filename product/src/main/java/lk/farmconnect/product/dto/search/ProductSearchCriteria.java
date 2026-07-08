package lk.farmconnect.product.dto.search;

import lk.farmconnect.product.entity.ProductType;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchCriteria(
        String keyword,
        ProductType productType,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,

        // NEW: Location Filters
        Integer locationDistrictId,
        Double lat,
        Double lon,
        Double radiusKm,

        // NEW: Delivery Filters
        Boolean isDeliveryAvailable,
        Integer deliveryDistrictId,

        // Pagination (Changed to Integer to allow null binding)
        Integer page,
        Integer size,
        String sortBy,
        String sortDir
) {
    // Compact constructor to handle nulls and enforce safety limits
    public ProductSearchCriteria {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1 || size > 50) size = 20; // Prevent DoS
        if (radiusKm == null || radiusKm <= 0) radiusKm = 50.0; // Default 50km
        if (sortDir == null || (!sortDir.equalsIgnoreCase("ASC") && !sortDir.equalsIgnoreCase("DESC"))) {
            sortDir = "DESC";
        }
    }
}