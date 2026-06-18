package lk.farmconnect.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.product.dto.ProductCreateRequest;
import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.mapper.ProductMapper;
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ProductMapper productMapper;


    @Transactional
    public ProductResponse createProduct(String productJson,
                                         List<MultipartFile> images,
                                         MultipartFile video,
                                         UUID farmerId) {

        // Safely Parse the JSON String
        ProductCreateRequest request;
        try {
            request = objectMapper.readValue(productJson, ProductCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid product JSON format. Please check your syntax.");
        }

        // Manually Trigger Validation
        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errorMessage);
        }

        // Strict File Validation
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one product image is required.");
        }

        // Upload Files to MinIO
        List<String> imageUrls = images.stream()
                .map(img -> fileStorageService.uploadFile(img, "products/images", true))
                .toList();

        String videoUrl = null;
        if (video != null && !video.isEmpty()) {
            videoUrl = fileStorageService.uploadFile(video, "products/videos", false);
        }

        // Save Product to Database
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        Product product = Product.builder()
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .attributes(request.attributes())
                .availableStock(request.availableStock())
                .minOrderQty(request.minOrderQty())
                .maxOrderQty(request.maxOrderQty())
                .qtyStep(request.qtyStep())
                .isDeliveryAvailable(request.isDeliveryAvailable())
                .expiryDate(request.expiryDate())
                .farmer(farmer)
                .imageUrls(imageUrls)
                .videoUrl(videoUrl)
                .build();

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return productMapper.toResponse(product);
    }

    // Security: Ensure only the owner can delete
    @Transactional
    public void softDeleteProduct(UUID productId, UUID requestingFarmerId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (!product.getFarmer().getId().equals(requestingFarmerId)) {
            throw new SecurityException("You do not have permission to delete this product");
        }

        product.setDeleted(true);
        productRepository.save(product);
        log.info("Product {} soft-deleted by farmer {}", productId, requestingFarmerId);
    }

}