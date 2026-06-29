package lk.farmconnect.user.dto;

import lk.farmconnect.user.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrivateUserResponse(
        UUID id,
        String name,
        String email,
        String mobileNumber,
        UserRole role,
        String profilePictureUrl,
        boolean profileComplete,
        Double lat,
        Double lon,
        String address,
        String city,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}