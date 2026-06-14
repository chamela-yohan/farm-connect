package lk.farmconnect.user.dto;

import lk.farmconnect.user.UserRole;
import java.util.UUID;

// SAFE FOR ANYONE TO SEE
public record PublicUserResponse(
        UUID id,
        String name,
        UserRole role,
        String profilePictureUrl,
        String city // Only show the city, NOT the exact lat/lon
) {}