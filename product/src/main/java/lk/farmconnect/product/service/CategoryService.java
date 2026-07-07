package lk.farmconnect.product.service;

import lk.farmconnect.product.dto.CategoryResponse;
import lk.farmconnect.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        log.debug("Fetching active categories for frontend");

        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getSlug()
                ))
                .toList();
    }
}