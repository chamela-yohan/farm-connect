package lk.farmconnect.common.service;

import lk.farmconnect.common.dto.CityResponse;
import lk.farmconnect.common.dto.DistrictResponse;
import lk.farmconnect.common.dto.ProvinceResponse;
import lk.farmconnect.common.repository.CityRepository;
import lk.farmconnect.common.repository.DistrictRepository;
import lk.farmconnect.common.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ProvinceRepository provinceRepo;
    private final DistrictRepository districtRepo;
    private final CityRepository cityRepo;

    @Transactional(readOnly = true)
    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepo.findAll().stream()
                .map(p -> new ProvinceResponse(p.getId(), p.getNameEn(), p.getNameSi(), p.getNameTa()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictResponse> getDistrictsByProvince(Integer provinceId) {
        return districtRepo.findByProvinceId(provinceId).stream()
                .map(d -> new DistrictResponse(d.getId(), d.getProvinceId(), d.getNameEn(), d.getNameSi(), d.getNameTa()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictResponse> getAllDistricts() {
        return districtRepo.findAll().stream()
                .map(d -> new DistrictResponse(d.getId(), d.getProvinceId(), d.getNameEn(), d.getNameSi(), d.getNameTa()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getCitiesByDistrict(Integer districtId) {
        return cityRepo.findByDistrictId(districtId).stream()
                .map(c -> new CityResponse(
                        c.getId(),
                        c.getDistrictId(), // We only send the ID, NOT the whole District object!
                        c.getNameEn(),
                        c.getNameSi(),
                        c.getNameTa(),
                        c.getPostcode(),
                        c.getLatitude(),
                        c.getLongitude(),
                        c.getSubNames()
                ))
                .toList();
    }
}