package lk.farmconnect.order.controller;

import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.order.dto.BuyerDashboardSummary;
import lk.farmconnect.order.dto.FarmerDashboardSummary;
import lk.farmconnect.order.service.DashboardService;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // FARMER ENDPOINT
    @GetMapping("/farmer")
    public ResponseEntity<ApiResponse<FarmerDashboardSummary>> getFarmerDashboard(
            @AuthenticationPrincipal User farmer) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getFarmerDashboard(farmer.getId())));
    }

    // BUYER ENDPOINT
    @GetMapping("/buyer")
    public ResponseEntity<ApiResponse<BuyerDashboardSummary>> getBuyerDashboard(
            @AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getBuyerDashboard(buyer.getId())));
    }
}