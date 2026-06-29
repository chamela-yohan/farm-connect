package lk.farmconnect.auth.controller;

import jakarta.validation.Valid;
import lk.farmconnect.auth.dto.*;
import lk.farmconnect.auth.service.AuthenticationService;
import lk.farmconnect.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.email());
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.email());
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("Token refresh request");
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // In a stateless JWT system, logout is handled client-side by deleting tokens
        // For complete logout, we would need to blacklist the token or delete refresh token from DB
        log.info("Logout request");
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}