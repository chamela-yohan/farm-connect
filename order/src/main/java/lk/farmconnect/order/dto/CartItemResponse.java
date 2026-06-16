package lk.farmconnect.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productTitle,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal subtotal
) {}