package lk.farmconnect.user.controller;

import jakarta.validation.Valid;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.user.User;
import lk.farmconnect.user.dto.ProfileUpdateRequest;
import lk.farmconnect.user.dto.PublicUserResponse;
import lk.farmconnect.user.dto.UserCreateRequest;
import lk.farmconnect.user.dto.PrivateUserResponse;
import lk.farmconnect.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<PrivateUserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        PrivateUserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PrivateUserResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {

        PrivateUserResponse response = userService.getMyPrivateProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getPublicProfile(
            @PathVariable("id") UUID id) {

        PublicUserResponse response = userService.getPublicProfile(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PrivateUserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Profile update request for user: {}", currentUser.getEmail());

        PrivateUserResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}