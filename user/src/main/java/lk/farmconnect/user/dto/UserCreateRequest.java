package lk.farmconnect.user.dto;

import jakarta.validation.constraints.*;
import lk.farmconnect.user.UserRole;

public record UserCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100) String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format") String email,

        @NotBlank(message = "Mobile number is required")
        String mobileNumber,

        @NotNull(message = "Role is required")
        UserRole role,

        @NotNull(message = "Latitude is required")
        @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,

        @NotNull(message = "Longitude is required")
        @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,

        String address,
        String city
) {}