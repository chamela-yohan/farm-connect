package lk.farmconnect.order.mapper;

import lk.farmconnect.order.dto.CartItemResponse;
import lk.farmconnect.order.dto.CartResponse;
import lk.farmconnect.order.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productTitle", source = "product.title")
    @Mapping(target = "productType", expression = "java(item.getProduct().getProductType().name())")
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "subtotal", expression = "java(item.getQuantity().multiply(item.getProduct().getPrice()))")
    CartItemResponse toCartItemResponse(CartItem item);

    CartResponse toCartResponse(List<CartItemResponse> items, BigDecimal totalAmount, int totalItems);
}