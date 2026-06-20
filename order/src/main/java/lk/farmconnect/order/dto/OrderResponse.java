package lk.farmconnect.order.dto;

import lk.farmconnect.order.entity.DeliveryMethod;
import lk.farmconnect.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID buyerId,
        String buyerName,
        UUID farmerId,
        String farmerName,
        OrderStatus status,
        BigDecimal totalAmount,
        DeliveryMethod deliveryMethod,
        String parentOrderRef,
        String invoiceUrl,
        String deliveryAddress,
        String buyerNotes,
        String farmerNotes,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {}