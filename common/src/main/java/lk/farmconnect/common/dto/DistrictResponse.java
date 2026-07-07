package lk.farmconnect.common.dto;

public record DistrictResponse(
        Integer id,
        Integer provinceId,
        String nameEn,
        String nameSi,
        String nameTa
) {}
