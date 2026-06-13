package lk.farmconnect.product.controller;

import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.product.dto.ProductCreateRequest;
import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.service.ProductService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

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

        ProductResponse response = productService.createProduct(productJson, images, video, farmer.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Secure endpoint: Only the owner can delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User farmer) {

        productService.softDeleteProduct(id, farmer.getId());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}