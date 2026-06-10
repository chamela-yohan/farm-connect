package lk.farmconnect.auth.dto;

import lk.farmconnect.user.UserRole;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo user
) {
    public record UserInfo(
            UUID id,
            String name,
            String email,
            UserRole role,
            String profilePictureUrl
    ){}
}
