package lk.farmconnect.product.mapper;

import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.common.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Component
public abstract class ProductMapper {

    @Autowired
    protected StorageService storageService;

    @Mapping(target = "farmerId", source = "farmer.id")
    @Mapping(target = "farmerName", source = "farmer.name")
    @Mapping(target = "lat", expression = "java(extractLat(product))")
    @Mapping(target = "lon", expression = "java(extractLon(product))")
    @Mapping(target = "imageUrls", expression = "java(generatePresignedImageUrls(product))")
    @Mapping(target = "videoUrl", expression = "java(generatePresignedVideoUrl(product))")
    public abstract ProductResponse toResponse(Product product);

    protected Double extractLat(Product product) {
        return product.getFarmer() != null && product.getFarmer().getLocation() != null
                ? product.getFarmer().getLocation().getY()
                : null;
    }

    protected Double extractLon(Product product) {
        return product.getFarmer() != null && product.getFarmer().getLocation() != null
                ? product.getFarmer().getLocation().getX()
                : null;
    }

    protected List<String> generatePresignedImageUrls(Product product) {
        if (product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
            return Collections.emptyList();
        }

        return product.getImageUrls().stream()
                .map(this::extractKeyFromUrl)
                .map(storageService::getPresignedUrl)
                .filter(url -> url != null)
                .collect(Collectors.toList());
    }

    protected String generatePresignedVideoUrl(Product product) {
        if (product.getVideoUrl() == null || product.getVideoUrl().isBlank()) {
            return null;
        }

        String key = extractKeyFromUrl(product.getVideoUrl());
        return storageService.getPresignedUrl(key);
    }

    protected String extractKeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        //  Check if it's actually a URL before processing
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            log.debug("Skipping non-URL string: {}", url);
            return url;  // Return as-is, don't try to extract
        }

        log.debug("Processing URL: {}", url);

        // Now we know it's a URL, extract the key
        String bucketPrefix = "/farmconnect-products/";
        int bucketIndex = url.indexOf(bucketPrefix);

        if (bucketIndex >= 0) {
            String key = url.substring(bucketIndex + bucketPrefix.length());
            log.debug("Extracted key: {}", key);
            return key;
        }

        log.warn("Bucket prefix not found in URL: {}", url);
        return url;
    }
}