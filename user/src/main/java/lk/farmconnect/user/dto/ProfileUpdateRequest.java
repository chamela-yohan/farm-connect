package lk.farmconnect.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lk.farmconnect.user.UserRole;

public record ProfileUpdateRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Mobile number is required")
        String mobileNumber,

        String address,
        String city,


        Double latitude,
        Double longitude,

        String profilePictureUrl
) {}