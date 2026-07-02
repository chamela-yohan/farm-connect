package lk.farmconnect.user.controller;

import jakarta.validation.Valid;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.common.service.StorageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<ApiResponse<PrivateUserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        PrivateUserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PrivateUserResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new RuntimeException("Authentication required"); // Will be caught by GlobalExceptionHandler
        }

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

        log.info("Updating profile for user: {}", currentUser.getEmail());
        log.info("Received request - Name: '{}', Mobile: '{}', City: '{}', Address: '{}', Lat: {}, Lon: {}",
                request.name(), request.mobileNumber(), request.city(), request.address(),
                request.latitude(), request.longitude());

        // If name is blank, log it
        if (request.name() == null || request.name().isBlank()) {
            log.error("Name is blank in request!");
        }

        try {
            PrivateUserResponse response = userService.updateProfile(currentUser.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to update profile for user: {}", currentUser.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/profile-picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        log.info("User {} is uploading a profile picture", currentUser.getEmail());

        // Upload to S3 and get the Object Key
        String s3Key = storageService.uploadFile(file, "users/profiles");

        // Save the Key to the database
        userService.updateProfilePicture(currentUser.getId(), s3Key);

        String presignedUrl = storageService.getPresignedUrl(s3Key);

        // Return the Key (or you can return a temporary URL, but key is fine)
        return ResponseEntity.ok(ApiResponse.success(s3Key));
    }

}