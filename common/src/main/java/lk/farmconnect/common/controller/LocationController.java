package lk.farmconnect.common.controller;

import lk.farmconnect.common.dto.CityResponse;
import lk.farmconnect.common.dto.DistrictResponse;
import lk.farmconnect.common.dto.ProvinceResponse;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.common.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getAllProvinces() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllProvinces()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistrictsByProvince(
            @RequestParam(required = false) Integer provinceId) {
        if (provinceId == null) {
            return ResponseEntity.ok(ApiResponse.success(locationService.getAllDistricts()));
        }
        return ResponseEntity.ok(ApiResponse.success(locationService.getDistrictsByProvince(provinceId)));
    }

    @GetMapping("/all-districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getAllDistricts() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllDistricts()));
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCitiesByDistrict(
            @RequestParam Integer districtId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getCitiesByDistrict(districtId)));
    }
}