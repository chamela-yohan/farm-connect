package lk.farmconnect.user.service;

import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.common.service.StorageService;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.dto.ProfileUpdateRequest;
import lk.farmconnect.user.dto.PublicUserResponse;
import lk.farmconnect.user.dto.UserCreateRequest;
import lk.farmconnect.user.dto.PrivateUserResponse;
import lk.farmconnect.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    // GeometryFactory is thread-safe, so it's safe to instantiate once
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final StorageService storageService;

    @Transactional
    public PrivateUserResponse createUser(UserCreateRequest request) {
        log.info("Creating new user with email: {}", request.email());

        // Convert Lat/Lon to PostGIS Point (JTS expects X=Lon, Y=Lat)
        Point location = geometryFactory.createPoint(new Coordinate(request.lon(), request.lat()));

        // Map Request to Entity
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .mobileNumber(request.mobileNumber())
                .role(request.role())
                .location(location)
                .address(request.address())
                .city(request.city())
                .build();

        // Save and Map to Response DTO
        User savedUser = userRepository.save(user);
        return mapToPrivateResponse(savedUser);
    }

    // Private
    @Transactional(readOnly = true)
    public PrivateUserResponse getMyPrivateProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToPrivateResponse(user);
    }

    // Public
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToPublicResponse(user);
    }

    // Mapper for Private Data
    private PrivateUserResponse mapToPrivateResponse(User user) {
        Double lat = user.getLocation() != null ? user.getLocation().getY() : null;
        Double lon = user.getLocation() != null ? user.getLocation().getX() : null;

        // Generate a fresh 1-hour presigned URL for the profile picture
        String profilePicUrl = null;
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
            profilePicUrl = storageService.getPresignedUrl(user.getProfilePictureUrl());
        }

        return new PrivateUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getRole(),
                profilePicUrl,
                user.getMobileNumber() != null && !user.getMobileNumber().trim().isEmpty(), // Compute profileComplete
                lat,
                lon,
                user.getAddress(),
                user.getCity(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // Mapper for Public Data (Notice: No email, no phone, no exact lat/lon)
    private PublicUserResponse mapToPublicResponse(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getProfilePictureUrl(),
                user.getCity()
        );
    }

    //Update Profile
    @Transactional
    public PrivateUserResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Updating profile for user: {}", user.getEmail());

        // Update basic fields
        user.setName(request.name());
        user.setMobileNumber(request.mobileNumber());
        user.setAddress(request.address());
        user.setCity(request.city());

        if (request.profilePictureUrl() != null && !request.profilePictureUrl().isBlank()) {
            // Check if it's an S3 key (short) or old presigned URL (long)
            if (request.profilePictureUrl().length() < 255) {
                // It's an S3 key, save it
                user.setProfilePictureUrl(request.profilePictureUrl());
            } else {
                // It's an old presigned URL, extract the key
                String key = extractKeyFromUrl(request.profilePictureUrl());
                user.setProfilePictureUrl(key);
            }
        }

        // Update Location (PostGIS Point) - Crucial for Farmers
        if (request.latitude() != null && request.longitude() != null) {
            org.locationtech.jts.geom.GeometryFactory geometryFactory =
                    new org.locationtech.jts.geom.GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(), 4326);
            org.locationtech.jts.geom.Point point = geometryFactory.createPoint(
                    new org.locationtech.jts.geom.Coordinate(request.longitude(), request.latitude())
            );
            user.setLocation(point);
        }

        User updatedUser = userRepository.save(user);

        return mapToPrivateResponse(updatedUser);
    }

    @Transactional
    public PrivateUserResponse updateProfilePicture(UUID userId, String s3Key) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Save the S3 KEY to the database, NOT the presigned URL
        user.setProfilePictureUrl(s3Key);
        userRepository.save(user);

        return mapToPrivateResponse(user);
    }

    // Helpers
    private String extractKeyFromUrl(String url) {
        if (url == null || url.isBlank()) return url;

        String bucketPrefix = "/farmconnect-products/";
        int bucketIndex = url.indexOf(bucketPrefix);
        if (bucketIndex >= 0) {
            return url.substring(bucketIndex + bucketPrefix.length());
        }
        return url;
    }
}