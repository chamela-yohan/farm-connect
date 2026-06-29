package lk.farmconnect.auth.service;

import lk.farmconnect.auth.dto.*;
import lk.farmconnect.auth.entity.RefreshToken;
import lk.farmconnect.auth.repository.RefreshTokenRepository;
import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok generates constructor for ALL final fields
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final GoogleAuthService googleAuthService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }

        log.info("Registering new user: {}", request.email());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .mobileNumber(request.mobileNumber())
                .role(request.role() != null ? request.role() : UserRole.BUYER)
                .build();

        userRepository.save(user);
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("User not found"));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        log.info("Google login attempt");
        User user = googleAuthService.authenticateWithGoogle(request.idToken(), userRepository);
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token not found in database"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return new AuthResponse(
                newAccessToken,
                refreshToken.getToken(),
                mapToUserInfo(user)
        );
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                mapToUserInfo(user)
        );
    }

    private AuthResponse.UserInfo mapToUserInfo(User user) {

        boolean isProfileComplete = user.getMobileNumber() != null && !user.getMobileNumber().trim().isEmpty();

        return new AuthResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getProfilePictureUrl(),
                isProfileComplete
        );
    }
}