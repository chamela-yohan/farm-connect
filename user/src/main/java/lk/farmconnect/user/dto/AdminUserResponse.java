package lk.farmconnect.user.dto;

import lk.farmconnect.user.UserRole; // Assuming you have a UserRole enum
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        String mobileNumber,
        UserRole role,
        boolean isVerified,
        boolean isAccountNonLocked, // Assuming you have this for suspension
        LocalDateTime createdAt
) {}