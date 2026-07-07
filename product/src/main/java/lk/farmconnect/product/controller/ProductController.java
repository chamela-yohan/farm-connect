package lk.farmconnect.product.controller;

import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.product.dto.CategoryResponse;
import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.dto.search.ProductSearchCriteria;
import lk.farmconnect.product.dto.search.ProductSearchRequest;
import lk.farmconnect.product.entity.Category;
import lk.farmconnect.product.repository.CategoryRepository;
import lk.farmconnect.product.service.CategoryService;
import lk.farmconnect.product.service.ProductService;
import lk.farmconnect.product.service.search.ProductSearchService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lk.farmconnect.product.dto.ProductUpdateRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;
    private final CategoryService  categoryService;


    // ==========================================
    // SEARCH & DISCOVERY (Public)
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @ModelAttribute ProductSearchRequest request) {

        log.info("Search request received from frontend");
        Page<ProductResponse> results = productSearchService.searchProducts(request);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ==========================================
    // STANDARD CRUD (Protected)
    // ==========================================

    // Public endpoint: Anyone can view products
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        System.out.println("Inside getProducts and page: " + page);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getAllActiveProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID id) {
        System.out.println("Inside getProduct and id: " + id);
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    // Secure endpoint: Only Farmers can create products
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestParam("product") String productJson,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @AuthenticationPrincipal User farmer) {

        if (farmer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        ProductResponse response = productService.createProduct(productJson, images, video, farmer.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Secure endpoint: Only the owner can delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User farmer) {

        if (farmer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        productService.softDeleteProduct(id, farmer.getId());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @RequestParam("product") String productJson,
            @AuthenticationPrincipal User farmer) {

        if (farmer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        log.info("Updating product: {} by farmer: {}", id, farmer.getEmail());
        ProductResponse product = productService.updateProduct(id, productJson, farmer.getId());
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/farmer/my-products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal User farmer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (farmer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        log.info("Fetching products for farmer: {}", farmer.getEmail());
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getFarmerProducts(farmer.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

}