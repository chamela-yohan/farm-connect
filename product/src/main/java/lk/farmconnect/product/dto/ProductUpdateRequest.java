package lk.farmconnect.product.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lk.farmconnect.product.entity.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ProductUpdateRequest(
        @Size(max = 255, message = "Title must be less than 255 characters")
        String title,
        String description,
        ProductType productType,
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,
        @NotNull(message = "Category is required")
        UUID categoryId,
        @DecimalMin(value = "0", message = "Min order qty cannot be negative")
        BigDecimal minOrderQty,
        @DecimalMin(value = "0", message = "Max order qty cannot be negative")
        BigDecimal maxOrderQty,
        @DecimalMin(value = "0", message = "Qty step cannot be negative")
        BigDecimal qtyStep,
        Boolean isDeliveryAvailable,
        @DecimalMin(value = "0", message = "Delivery fee cannot be negative")
        BigDecimal deliveryFee,
        @NotEmpty(message = "At least one location city is required")
        List<Integer> locationCityIds,
        Set<Integer> deliveryDistrictIds,
        JsonNode attributes
) {
    public ProductUpdateRequest {
        if (productType == ProductType.PHYSICAL_GOOD) {
            if (attributes == null || attributes.get("availableStock") == null) {
                throw new IllegalArgumentException("Available stock is required for physical goods");
            }
            if (attributes.get("unit") == null || attributes.get("unit").asText().isBlank()) {
                throw new IllegalArgumentException("Unit is required for physical goods");
            }
        }

        if (productType == ProductType.RENTABLE) {
            if (attributes == null || attributes.get("rentalPricePerDay") == null) {
                throw new IllegalArgumentException("Rental price per day is required");
            }
            if (attributes.get("minRental") == null) {
                throw new IllegalArgumentException("Minimum rental period is required");
            }
        }

        if (productType == ProductType.SERVICE) {
            if (attributes == null || attributes.get("minRental") == null) {
                throw new IllegalArgumentException("Minimum booking duration is required");
            }
        }
    }

}
