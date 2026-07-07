package lk.farmconnect.product.repository;

import lk.farmconnect.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    // Only fetch active categories for the frontend dropdown
    List<Category> findByIsActiveTrueOrderByNameAsc();
}