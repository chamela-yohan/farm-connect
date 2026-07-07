package lk.farmconnect.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provinces")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Province {
    @Id
    private Integer id;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "name_si")
    private String nameSi;

    @Column(name = "name_ta")
    private String nameTa;
}