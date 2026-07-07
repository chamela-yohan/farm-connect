package lk.farmconnect.common.repository;

import lk.farmconnect.common.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Integer> {
    List<District> findByProvinceId(Integer provinceId);
}