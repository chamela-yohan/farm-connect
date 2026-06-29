package lk.farmconnect.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lk.farmconnect.user.UserRole;

public record ProfileUpdateRequest(
        @NotBlank(message = "Mobile number is required")
        String mobileNumber,

        @NotNull(message = "Role is required")
        UserRole role
) {}