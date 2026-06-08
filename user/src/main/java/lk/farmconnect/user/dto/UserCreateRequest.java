package lk.farmconnect.user.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid mobile number format")
        String mobileNumber,

        @NotNull(message = "Role is required")
        UserRoleDTO role,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double lat,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double lon,

        @Size(max = 255, message = "Address is too long")
        String address,

        @Size(max = 100, message = "City name is too long")
        String city
) {}