package lk.farmconnect.user.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String mobileNumber,
        UserRoleDTO role,
        String profilePictureUrl,
        Double lat,
        Double lon,
        String address,
        String city,
        LocalDateTime createdAt
) {}