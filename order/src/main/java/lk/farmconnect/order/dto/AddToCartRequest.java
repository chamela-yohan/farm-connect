package lk.farmconnect.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record AddToCartRequest(
        @NotNull UUID productId,
        @NotNull @Positive BigDecimal quantity
) {}