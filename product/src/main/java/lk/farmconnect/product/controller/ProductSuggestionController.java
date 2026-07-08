package lk.farmconnect.product.controller;

import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductSuggestionController {

    private final ProductRepository productRepository;

    @GetMapping("/keyword-suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getKeywordSuggestions(
            @RequestParam String query) {
        if (query == null || query.length() < 2) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success(productRepository.findKeywordSuggestions(query)));
    }
}