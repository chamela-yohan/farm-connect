package lk.farmconnect.product.dto.search;

import lk.farmconnect.product.entity.ProductType;

public record ProductSearchCriteria(
        String keyword,
        String category,
        ProductType productType, // NEW
        Double lat,
        Double lon,
        Double radiusKm,
        Double minPrice,
        Double maxPrice,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir,
        Boolean isDeliveryAvailable
) {
    public ProductSearchCriteria {
        if (page == null) page = 0;
        if (size == null) size = 12;
        if (radiusKm == null) radiusKm = 50.0;
        if (sortDir == null) sortDir = "DESC";
    }
}