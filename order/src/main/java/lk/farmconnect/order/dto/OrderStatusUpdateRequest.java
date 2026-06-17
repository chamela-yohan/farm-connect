package lk.farmconnect.order.dto;

import jakarta.validation.constraints.NotNull;
import lk.farmconnect.order.entity.OrderStatus;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus newStatus,
        String notes // e.g., Rejection reason
) {}