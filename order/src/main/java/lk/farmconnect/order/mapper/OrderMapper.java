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

    // CONDITIONAL UNLOCKING: Only expose mobile numbers if status >= ACCEPTED
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.name")
    @Mapping(target = "buyerMobile", expression = "java(order.getStatus().compareTo(OrderStatus.ACCEPTED) >= 0 ? order.getBuyer().getMobileNumber() : null)")

    @Mapping(target = "farmerId", source = "farmer.id")
    @Mapping(target = "farmerName", source = "farmer.name")
    @Mapping(target = "farmerMobile", expression = "java(order.getStatus().compareTo(OrderStatus.ACCEPTED) >= 0 ? order.getFarmer().getMobileNumber() : null)")
    OrderResponse toOrderResponse(Order order);
}