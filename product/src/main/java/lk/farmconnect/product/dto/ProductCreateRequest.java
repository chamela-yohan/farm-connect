package lk.farmconnect.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ProductCreateRequest(
        @NotBlank String title,
        String description,
        Map<String, Object> attributes,

        @NotNull @DecimalMin("0.01") BigDecimal availableStock,
        @DecimalMin("0.01") BigDecimal minOrderQty, // Optional
        @DecimalMin("0.01") BigDecimal maxOrderQty, // Optional
        @NotNull @DecimalMin("0.01") BigDecimal qtyStep,
        @NotNull(message = "Please tell us whether delivery is available or not.")  boolean isDeliveryAvailable,
        @DecimalMin("0") BigDecimal deliveryFee,
        @Future LocalDate expiryDate, // Optional, but if present must be in the future
        @NotNull @DecimalMin("0.01") BigDecimal price
) {
    @AssertTrue(message = "Delivery fee is mandatory when selecting DELIVERY")
    public boolean isDeliveryFeeValid() {
        if (isDeliveryAvailable) {
            return deliveryFee != null;
        }
        return true; // If PICKUP, Fee is optional/ignored
    }
}