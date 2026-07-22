package lk.farmconnect.user.controller;

import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.user.dto.AdminUserResponse;
import lk.farmconnect.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getAllUsers(pageable)));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> toggleStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.toggleUserStatus(userId)));
    }

    @PutMapping("/{userId}/verify")
    public ResponseEntity<ApiResponse<AdminUserResponse>> verifyFarmer(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.verifyFarmer(userId)));
    }
}