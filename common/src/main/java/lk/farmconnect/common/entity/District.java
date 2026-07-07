package lk.farmconnect.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "districts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class District {
    @Id
    private Integer id;

    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "name_si")
    private String nameSi;

    @Column(name = "name_ta")
    private String nameTa;
}