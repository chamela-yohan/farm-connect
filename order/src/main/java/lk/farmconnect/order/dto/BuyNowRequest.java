package lk.farmconnect.order.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lk.farmconnect.order.entity.DeliveryMethod;
import java.math.BigDecimal;
import java.util.UUID;

public record BuyNowRequest(
        @NotNull UUID productId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull DeliveryMethod deliveryMethod,
        String deliveryAddress,
        String buyerNotes
) {
    // Cross-Field Validation: Address required if DELIVERY
    @AssertTrue(message = "Delivery address is mandatory when selecting DELIVERY")
    public boolean isDeliveryAddressValid() {
        if (deliveryMethod == DeliveryMethod.DELIVERY) {
            return deliveryAddress != null && !deliveryAddress.isBlank();
        }
        return true;
    }
}