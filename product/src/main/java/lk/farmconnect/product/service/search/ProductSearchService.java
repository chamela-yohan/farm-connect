package lk.farmconnect.product.service.search;

import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.dto.search.ProductSearchRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest request) {
        log.info("Executing advanced search: keyword={}, type={}, lat={}", request.keyword(), request.productType(), request.latitude());

        // 1. Build Pagination & Sorting
        Pageable pageable = buildPageable(request);

        // 2. Execute Native Query
        Page<Product> products = productRepository.searchProducts(
                request.keyword(),
                request.productType(),
                request.category(),
                request.minPrice(),
                request.maxPrice(),
                request.latitude(),
                request.longitude(),
                request.radiusKm(),
                request.city(),
                request.minStock(),
                request.minRentalDays(),
                request.maxRentalDays(),
                request.deliveryAvailable(),
                pageable
        );

        // 3. Map to Response (Handles JSONB, Presigned URLs, etc.)
        return products.map(productMapper::toResponse);
    }

    private Pageable buildPageable(ProductSearchRequest request) {
        String sortBy = request.sortBy() != null ? request.sortBy() : "created_at";
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.sortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Whitelist safe columns to prevent SQL injection via sort parameter
        if (!sortBy.equals("price") && !sortBy.equals("created_at") && !sortBy.equals("title")) {
            sortBy = "created_at";
        }

        return PageRequest.of(request.page(), request.size(), Sort.by(direction, sortBy));
    }
}