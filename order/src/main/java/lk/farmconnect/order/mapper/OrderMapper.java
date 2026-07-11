package lk.farmconnect.order.mapper;

import lk.farmconnect.common.service.StorageService; //  Import from common module
import lk.farmconnect.order.dto.OrderItemResponse;
import lk.farmconnect.order.dto.OrderResponse;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.entity.OrderItem;
import lk.farmconnect.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

//  Changed from interface to abstract class to allow dependency injection
@Mapper(componentModel = "spring")
public abstract class OrderMapper {

    @Autowired
    protected StorageService storageService; //  Injected from common module

    @Mapping(target = "items", source = "items")
    public abstract OrderResponse toOrderResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productTitle", source = "product.title")
    @Mapping(target = "unitPrice", source = "product.price")

    //  Use expression to call our custom method
    @Mapping(target = "imageUrls", expression = "java(mapImageUrls(item.getProduct()))")

    @Mapping(target = "attributesSnapshot", source = "product.attributes")
    public abstract OrderItemResponse toOrderItemResponse(OrderItem item);

    //  Custom method to safely extract keys and generate Presigned URLs
    protected List<String> mapImageUrls(Product product) {
        if (product == null || product.getImages() == null) {
            return Collections.emptyList();
        }
        return product.getImages().stream()
                .sorted((i1, i2) -> Integer.compare(i1.getDisplayOrder(), i2.getDisplayOrder()))
                .map(img -> storageService.getPresignedUrl(img.getImageUrl()))
                .collect(Collectors.toList());
    }
}