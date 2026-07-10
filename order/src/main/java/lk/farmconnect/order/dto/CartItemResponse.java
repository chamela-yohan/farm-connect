package lk.farmconnect.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productTitle,
        String productType,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal subtotal,
        List<String> imageUrls,      //  For the UI
        BigDecimal qtyStep,          //  To increment/decrement correctly
        BigDecimal minOrderQty,      //  Frontend validation
        BigDecimal maxOrderQty,      //  Frontend validation
        BigDecimal availableStock    //  Frontend validation
) {}