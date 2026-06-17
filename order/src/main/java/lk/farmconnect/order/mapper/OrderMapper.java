package lk.farmconnect.order.mapper;

import lk.farmconnect.order.dto.OrderItemResponse;
import lk.farmconnect.order.dto.OrderResponse;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productTitle", source = "productTitleSnapshot")
    @Mapping(target = "unitPrice", source = "unitPriceSnapshot")
    OrderItemResponse toOrderItemResponse(OrderItem item);

    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.name")
    @Mapping(target = "farmerId", source = "farmer.id")
    @Mapping(target = "farmerName", source = "farmer.name")
    OrderResponse toOrderResponse(Order order);
}