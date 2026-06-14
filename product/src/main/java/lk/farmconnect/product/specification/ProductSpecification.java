package lk.farmconnect.product.specification;

import lk.farmconnect.product.dto.search.ProductSearchCriteria;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.entity.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    // Private constructor to prevent instantiation
    private ProductSpecification() {}

    public static Specification<Product> buildSpecification(ProductSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {

            // Base Predicates: Always ensure we only show active, non-deleted products
            // This is crucial when using the standard findAll(Specification, Pageable) method
            var predicates = criteriaBuilder.conjunction();

            predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.isFalse(root.get("isDeleted")));
            predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));

            // Dynamic Keyword Search (Case-Insensitive)
            // Searches in BOTH title and description
            if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
                String likePattern = "%" + criteria.keyword().toLowerCase() + "%";

                var titlePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), likePattern);
                var descPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), likePattern);

                // Combine with OR: title LIKE '%keyword%' OR description LIKE '%keyword%'
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.or(titlePredicate, descPredicate));
            }

            // Dynamic Price Filtering
            if (criteria.minPrice() != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }

            if (criteria.maxPrice() != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            return predicates;
        };
    }
}