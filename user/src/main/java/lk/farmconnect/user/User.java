package lk.farmconnect.user;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String role;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    public User() {}

    public User(String name, String role, Point location) {
        this.name = name;
        this.role = role;
        this.location = location;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }


}
