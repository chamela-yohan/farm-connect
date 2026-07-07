package lk.farmconnect.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lk.farmconnect.common.entity.City;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.common.repository.CityRepository;
import lk.farmconnect.common.service.StorageService;
import lk.farmconnect.product.dto.ProductCreateRequest;
import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.dto.ProductUpdateRequest;
import lk.farmconnect.product.entity.*;
import lk.farmconnect.product.mapper.ProductMapper;
import lk.farmconnect.product.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository; // From common module

    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(String productJson,
                                         List<MultipartFile> images,
                                         MultipartFile video,
                                         UUID farmerId) {

        // 1. Parse and Validate JSON
        ProductCreateRequest request;
        try {
            request = objectMapper.readValue(productJson, ProductCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid product JSON format");
        }

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errorMessage);
        }

        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one product image is required");
        }

        // 2. Upload Media
        List<String> imageKeys = images.stream()
                .map(img -> storageService.uploadFile(img, "products/images", StorageService.FileType.IMAGE))
                .toList();

        String videoKey = null;
        if (video != null && !video.isEmpty()) {
            videoKey = storageService.uploadFile(video, "products/videos", StorageService.FileType.VIDEO);
        }

        // 3. Fetch Relationships
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // 4. Map to Entity
        Product product = productMapper.toEntity(request);
        product.setFarmer(farmer);
        product.setCategory(category);
        product.setVideoUrl(videoKey);

        // 5. Handle Delivery Areas (Set of District IDs)
        if (request.deliveryDistrictIds() != null) {
            product.setDeliveryDistrictIds(request.deliveryDistrictIds());
        }

        // 6. Handle Product Locations (Multiple Cities)
        if (request.locationCityIds() != null && !request.locationCityIds().isEmpty()) {
            Set<ProductLocation> locations = request.locationCityIds().stream()
                    .map(cityId -> {
                        City city = cityRepository.findById(cityId)
                                .orElseThrow(() -> new ResourceNotFoundException("City not found with ID: " + cityId));
                        return ProductLocation.builder()
                                .product(product)
                                .city(city)
                                .build();
                    })
                    .collect(Collectors.toSet());
            product.setLocations(locations);
        }

        // 7. Handle Images
        List<ProductImage> productImages = imageKeys.stream()
                .map(key -> ProductImage.builder()
                        .product(product)
                        .imageUrl(key)
                        .displayOrder(imageKeys.indexOf(key))
                        .build())
                .collect(Collectors.toList());
        product.setImages(productImages);

        // 8. Save and Return
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully: {} by farmer {}", savedProduct.getId(), farmerId);

        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActiveProducts(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getFarmerProducts(UUID farmerId, Pageable pageable) {
        return productRepository.findByFarmerIdAndIsDeletedFalse(farmerId, pageable)
                .map(productMapper::toResponse);
    }

    @Transactional
    public void softDeleteProduct(UUID productId, UUID requestingFarmerId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getFarmer().getId().equals(requestingFarmerId)) {
            throw new SecurityException("You do not have permission to delete this product");
        }

        product.setDeleted(true);
        productRepository.save(product);
        log.info("Product {} soft-deleted by farmer {}", productId, requestingFarmerId);
    }

    @Transactional
    public ProductResponse updateProduct(UUID productId, String productJson, UUID farmerId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getFarmer().getId().equals(farmerId)) {
            throw new SecurityException("You don't have permission to update this product");
        }

        ProductUpdateRequest request;
        try {
            request = objectMapper.readValue(productJson, ProductUpdateRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid product JSON format");
        }

        // Update basic fields
        productMapper.updateEntity(product, request);

        // Update Category if changed
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        // Update Delivery Areas if provided
        if (request.deliveryDistrictIds() != null) {
            product.setDeliveryDistrictIds(request.deliveryDistrictIds());
        }

        // Update Locations if provided
        if (request.locationCityIds() != null) {
            product.getLocations().clear(); // Clear old locations
            if (!request.locationCityIds().isEmpty()) {
                Set<ProductLocation> newLocations = request.locationCityIds().stream()
                        .map(cityId -> {
                            City city = cityRepository.findById(cityId)
                                    .orElseThrow(() -> new ResourceNotFoundException("City not found with ID: " + cityId));
                            return ProductLocation.builder()
                                    .product(product)
                                    .city(city)
                                    .build();
                        })
                        .collect(Collectors.toSet());
                product.setLocations(newLocations);
            }
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product {} updated by farmer {}", productId, farmerId);

        return productMapper.toResponse(updatedProduct);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return productRepository.findDistinctCategories();
    }
}