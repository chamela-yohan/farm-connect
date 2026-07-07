package lk.farmconnect.common.repository;

import lk.farmconnect.common.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CityRepository extends JpaRepository<City, Integer> {
    List<City> findByDistrictId(Integer districtId);
}