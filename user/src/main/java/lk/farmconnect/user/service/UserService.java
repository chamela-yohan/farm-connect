package lk.farmconnect.user.service;

import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
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

        return new PrivateUserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getMobileNumber(),
                user.getRole(), user.getProfilePictureUrl(), lat, lon,
                user.getAddress(), user.getCity(), user.getCreatedAt()
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
}