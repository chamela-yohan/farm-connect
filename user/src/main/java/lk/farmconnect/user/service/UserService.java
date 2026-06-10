package lk.farmconnect.user.service;

import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.dto.UserCreateRequest;
import lk.farmconnect.user.dto.UserResponse;
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
    public UserResponse createUser(UserCreateRequest request) {
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
        return mapToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    // Private mapper to ensure JTS Point is NEVER exposed to the JSON serializer
    private UserResponse mapToResponse(User user) {
        Double lat = user.getLocation() != null ? user.getLocation().getY() : null;
        Double lon = user.getLocation() != null ? user.getLocation().getX() : null;

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getRole(),
                user.getProfilePictureUrl(),
                lat,
                lon,
                user.getAddress(),
                user.getCity(),
                user.getCreatedAt()
        );
    }
}