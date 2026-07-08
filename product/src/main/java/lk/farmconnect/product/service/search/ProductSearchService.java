package lk.farmconnect.product.service.search;

import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.dto.search.ProductSearchCriteria;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.mapper.ProductMapper;
import lk.farmconnect.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // Whitelist to prevent SQL injection via sort parameter
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("price", "created_at", "title");

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchCriteria criteria) {
        log.info("Executing advanced search: keyword={}, type={}, district={}, delivery={}",
                criteria.keyword(), criteria.productType(), criteria.locationDistrictId(), criteria.isDeliveryAvailable());

        log.info(
                "Locations: lat={}, lon={}, radius={}",
                criteria.lat(),
                criteria.lon(),
                criteria.radiusKm()
        );


        // 1. Build Safe Pagination & Sorting
        Pageable pageable = buildPageable(criteria);


        // 2. Execute Native Query
        Page<Product> products = productRepository.searchProductsAdvanced(
                criteria.keyword(),
                criteria.categoryId(),
                // Convert Enum to String to prevent native query binding issues
                criteria.productType() != null ? criteria.productType().name() : null,

                criteria.minPrice(),
                criteria.maxPrice(),

                // Location Params
                criteria.locationDistrictId(),
                criteria.lat(),
                criteria.lon(),
                criteria.radiusKm(),

                // Delivery Params
                criteria.isDeliveryAvailable(),
                criteria.deliveryDistrictId(),

                pageable
        );

        // 3. Map to Response (Handles JSONB, Presigned URLs, etc.)
        return products.map(productMapper::toResponse);
    }

    private Pageable buildPageable(ProductSearchCriteria criteria) {
        String sortBy = criteria.sortBy() != null ? criteria.sortBy() : "created_at";

        // Enforce whitelist
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "created_at";
        }

        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.sortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, sortBy));
    }
}