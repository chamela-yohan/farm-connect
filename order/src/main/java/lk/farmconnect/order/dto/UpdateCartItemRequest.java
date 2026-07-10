package lk.farmconnect.order.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        BigDecimal quantity
) {}