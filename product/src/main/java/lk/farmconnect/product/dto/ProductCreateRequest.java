package lk.farmconnect.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;

public record ProductCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Positive BigDecimal price,
        Map<String, Object> attributes // Dynamic JSONB data
) {}