package lk.farmconnect.order.dto;

import jakarta.validation.constraints.NotNull;
import lk.farmconnect.order.entity.DeliveryMethod;

public record OrderCreateRequest(
        @NotNull DeliveryMethod deliveryMethod,
        String deliveryAddress, // Required if DELIVERY
        String buyerNotes
) {}