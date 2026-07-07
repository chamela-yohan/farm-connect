package lk.farmconnect.product.mapper;

import lk.farmconnect.common.service.StorageService;
import lk.farmconnect.product.dto.ProductCreateRequest;
import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.dto.ProductUpdateRequest;
import lk.farmconnect.product.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final StorageService storageService;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^/]+/[^/]+/(.*)");

    // Request -> Entity
    public Product toEntity(ProductCreateRequest request) {
        return Product.builder()
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .productType(request.productType())
                .minOrderQty(request.minOrderQty())
                .maxOrderQty(request.maxOrderQty())
                .qtyStep(request.qtyStep())
                .isDeliveryAvailable(request.isDeliveryAvailable() != null && request.isDeliveryAvailable())
                .deliveryFee(request.deliveryFee())
                .attributes(request.attributes())
                .deliveryDistrictIds(request.deliveryDistrictIds() != null ? request.deliveryDistrictIds() : Collections.emptySet())
                .build();
    }

    // Update Entity (Ignores null fields)
    public void updateEntity(Product product, ProductUpdateRequest request) {
        if (request.title() != null) product.setTitle(request.title());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.productType() != null) product.setProductType(request.productType());
        if (request.minOrderQty() != null) product.setMinOrderQty(request.minOrderQty());
        if (request.maxOrderQty() != null) product.setMaxOrderQty(request.maxOrderQty());
        if (request.qtyStep() != null) product.setQtyStep(request.qtyStep());
        if (request.isDeliveryAvailable() != null) product.setDeliveryAvailable(request.isDeliveryAvailable());
        if (request.deliveryFee() != null) product.setDeliveryFee(request.deliveryFee());
        if (request.attributes() != null) product.setAttributes(request.attributes());
        if (request.deliveryDistrictIds() != null) product.setDeliveryDistrictIds(request.deliveryDistrictIds());
    }

    // Entity -> Response
    public ProductResponse toResponse(Product product) {
        // Map Images to Presigned URLs
        List<String> imageUrls = product.getImages() != null
                ? product.getImages().stream()
                .sorted((i1, i2) -> Integer.compare(i1.getDisplayOrder(), i2.getDisplayOrder()))
                .map(img -> extractKey(img.getImageUrl()))
                .map(storageService::getPresignedUrl)
                .filter(url -> url != null)
                .collect(Collectors.toList())
                : Collections.emptyList();

        // Map Video to Presigned URL
        String videoUrl = product.getVideoUrl() != null
                ? storageService.getPresignedUrl(extractKey(product.getVideoUrl()))
                : null;

        // Map Locations
        List<ProductResponse.LocationDetail> locationDetails = product.getLocations() != null
                ? product.getLocations().stream()
                .map(loc -> new ProductResponse.LocationDetail(
                        loc.getCity().getId(),
                        loc.getCity().getNameEn(),
                        loc.getCity().getDistrictId(),
                        null // District name can be resolved by frontend or fetched via a join
                ))
                .collect(Collectors.toList())
                : Collections.emptyList();

        // Map Category
        UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;

        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getProductType(),
                product.getStatus(),
                product.getMinOrderQty(),
                product.getMaxOrderQty(),
                product.getQtyStep(),
                product.isDeliveryAvailable(),
                product.getDeliveryFee(),
                categoryId,
                categoryName,
                product.getAttributes(),
                imageUrls,
                videoUrl,
                locationDetails,
                product.getDeliveryDistrictIds(),
                product.getFarmer().getId(),
                product.getFarmer().getName(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    // Helper to extract S3 key from full URL (Legacy data fix)
    private String extractKey(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) return urlOrKey;
        Matcher matcher = URL_PATTERN.matcher(urlOrKey);
        return matcher.matches() ? matcher.group(1) : urlOrKey;
    }
}