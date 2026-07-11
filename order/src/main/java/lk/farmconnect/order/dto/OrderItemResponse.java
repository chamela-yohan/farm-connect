package lk.farmconnect.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productTitle,
        BigDecimal unitPrice,
        BigDecimal requestedQty,
        BigDecimal approvedQty,
        BigDecimal subtotal,
        List<String> imageUrls,
        JsonNode attributesSnapshot
) {}