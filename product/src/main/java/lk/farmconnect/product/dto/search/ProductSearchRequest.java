package lk.farmconnect.product.dto.search;

import lk.farmconnect.product.entity.ProductType;
import java.math.BigDecimal;

public record ProductSearchRequest(
        // Text & Type
        String keyword,
        ProductType productType,
        String category, // From JSONB attributes->>'category'

        // Price
        BigDecimal minPrice,
        BigDecimal maxPrice,

        // Location (PostGIS)
        Double latitude,
        Double longitude,
        Double radiusKm,
        String city, // Fallback if no GPS

        // Dynamic Type-Specific Filters (Stored in JSONB)
        BigDecimal minStock,          // For PHYSICAL_GOOD
        Integer minRentalDays,        // For RENTABLE
        Integer maxRentalDays,        // For RENTABLE
        Boolean deliveryAvailable,

        // Pagination & Sorting
        int page,
        int size,
        String sortBy,
        String sortDir
) {
    public ProductSearchRequest {
        // Safety defaults to prevent DB crashes
        if (page < 0) page = 0;
        if (size < 1 || size > 50) size = 20; // Max 50 items per page
        if (radiusKm == null) radiusKm = 50.0;
        if (sortDir == null || (!sortDir.equalsIgnoreCase("ASC") && !sortDir.equalsIgnoreCase("DESC"))) {
            sortDir = "DESC";
        }
    }
}