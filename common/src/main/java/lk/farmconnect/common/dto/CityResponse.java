package lk.farmconnect.common.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record CityResponse(
        Integer id,
        Integer districtId,
        String nameEn,
        String nameSi,
        String nameTa,
        String postcode,
        Double latitude,
        Double longitude,
        JsonNode subNames // Jackson handles JsonNode perfectly
) {}