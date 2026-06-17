package lk.farmconnect.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productTitle,
        BigDecimal unitPrice,
        BigDecimal requestedQty,
        BigDecimal approvedQty,
        BigDecimal subtotal
) {}