package lk.farmconnect.product.service.search;

import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.dto.search.ProductSearchCriteria;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.mapper.ProductMapper;
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.product.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("createdAt", "price", "title");

    @Transactional(readOnly = true)
    @Cacheable(value = "productSearch", key = "#criteria")
    public Page<ProductResponse> searchProducts(ProductSearchCriteria criteria) {
        log.info("Executing search with criteria: {}", criteria);

        // Build Safe Pagination and sorting
        Pageable pageable = buildPageable(criteria);

        // Build Dynamic SQL WHERE Clause (Keyword, Price, etc.)
        var specification = ProductSpecification.buildSpecification(criteria);

        // Execute Query (Repository handles the DB hit + N+1 prevention via EntityGraph)
        Page<Product> productPage = productRepository.findAll(specification,pageable);

        // Map Entity to DTO
        return productPage.map(productMapper::toResponse);

    }

    // Get distinct categories
    @Transactional(readOnly = true)
    @Cacheable(value = "productCategories")
    public List<String> getDistinctCategories() {
        log.info("Fetching distinct categories");
        return productRepository.findDistinctCategories();
    }


    // ==========================================
    // Helpers
    // ==========================================
    private Pageable buildPageable(ProductSearchCriteria criteria) {

        Sort sort = Sort.by(Sort.Direction.fromString(criteria.sortDir()), criteria.sortBy());

        if (!ALLOWED_SORT_FIELDS.contains(criteria.sortBy())) {
            log.warn("Invalid sort field requested: {}. Defaulting to createdAt.", criteria.sortBy());
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(criteria.page(), criteria.size(), sort);

    }


}
