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
            var predicates = criteriaBuilder.conjunction();

            // Base: Active, non-deleted products
            predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.isFalse(root.get("isDeleted")));
            predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));

            // Keyword search
            if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
                String likePattern = "%" + criteria.keyword().toLowerCase() + "%";
                var titlePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), likePattern);
                var descPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), likePattern);
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.or(titlePredicate, descPredicate));
            }

            // Price filtering
            if (criteria.minPrice() != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }

            if (criteria.maxPrice() != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            // Category filtering (JSON attributes)
            if (criteria.category() != null && !criteria.category().isBlank()) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(
                                criteriaBuilder.function("jsonb_extract_path_text", String.class,
                                        root.get("attributes"), criteriaBuilder.literal("category")),
                                criteria.category()
                        ));
            }

            // Location-based radius filtering (PostGIS)
            if (criteria.lat() != null && criteria.lon() != null && criteria.radiusKm() != null) {
                double radiusMeters = criteria.radiusKm() * 1000;
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.lessThanOrEqualTo(
                                criteriaBuilder.function("ST_Distance", Double.class,
                                        root.get("farmer").get("location"),
                                        criteriaBuilder.function("ST_GeogFromText", Object.class,
                                                criteriaBuilder.literal("POINT(" + criteria.lon() + " " + criteria.lat() + ")"))),
                                radiusMeters
                        ));
            }

            return predicates;
        };
    }
}