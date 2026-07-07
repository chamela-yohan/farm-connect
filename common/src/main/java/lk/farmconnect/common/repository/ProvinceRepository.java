package lk.farmconnect.common.repository;

import lk.farmconnect.common.entity.District;
import lk.farmconnect.common.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvinceRepository extends JpaRepository<Province,Integer> {
}
