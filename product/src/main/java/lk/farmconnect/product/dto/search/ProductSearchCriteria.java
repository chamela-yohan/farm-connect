package lk.farmconnect.product.dto.search;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        Double lat,
        Double lon,
        Double radiusKm,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int page,
        int size,
        String sortBy,
        String sortDir
) {
    public ProductSearchCriteria {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 20; // Strict pagination limit (Prevents DoS)
        if (radiusKm == null || radiusKm <= 0) radiusKm = 15.0; // Default 15km radius
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDir == null || sortDir.isBlank()) sortDir = "desc";
    }
}