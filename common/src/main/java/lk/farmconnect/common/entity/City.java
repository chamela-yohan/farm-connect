package lk.farmconnect.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "cities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class City {
    @Id
    private Integer id;

    @Column(name = "district_id")
    private Integer districtId;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "name_si")
    private String nameSi;

    @Column(name = "name_ta")
    private String nameTa;

    private String postcode;
    private Double latitude;
    private Double longitude;

    // PostGIS Geography Point (For the 10km radius search)
    @JsonIgnore
    @Column(columnDefinition = "geography(Point, 4326)")
    private Point coordinates;

    // NEW: JSONB column for sub-names (e.g., {"en": "Fort", "si": "කොටුව"})
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sub_names", columnDefinition = "jsonb")
    private JsonNode subNames;
}