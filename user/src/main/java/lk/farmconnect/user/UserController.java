package lk.farmconnect.user;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public User createUser(
            @RequestParam("name") String name,
            @RequestParam("role") String role,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {

        // Convert standard Lat/Lon into a PostGIS Point
        Point location = geometryFactory.createPoint(new Coordinate(lon, lat));

        User newUser = new User(name, role, location);
        return userRepository.save(newUser);
    }
}
