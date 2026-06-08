package lk.farmconnect.user.service;

import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.UserRole;
import lk.farmconnect.user.dto.UserCreateRequest;
import lk.farmconnect.user.dto.UserResponse;
import lk.farmconnect.user.dto.UserRoleDTO;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public UserResponse createUser(UserCreateRequest request){
        UserRole userRole = UserRole.valueOf(request.role().name());
        Point location = geometryFactory.createPoint(new Coordinate(request.lon(),request.lat()));
        User  user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setMobileNumber(request.mobileNumber());
        user.setRole(userRole);
        user.setLocation(location);
        user.setAddress(request.address());
        user.setCity(request.city());
        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    private UserResponse mapToResponse(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNumber(),
                UserRoleDTO.valueOf(user.getRole().name()),
                user.getProfilePictureUrl(),
                user.getLocation().getY(), // Y - Latitude
                user.getLocation().getX(),  // X - Longitude
                user.getAddress(),
                user.getCity(),
                user.getCreatedAt()
        );
    }
}
