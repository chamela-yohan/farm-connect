package lk.farmconnect.user.service;

import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.UserRole;
import lk.farmconnect.user.dto.AdminUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToAdminResponse);
    }

    @Transactional
    public AdminUserResponse toggleUserStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Prevent admin from locking themselves out
        if (user.getId().equals(getCurrentAdminId())) { // Implement this helper or use SecurityContext
            throw new BusinessException("You cannot suspend your own account.");
        }

        user.setAccountNonLocked(!user.isAccountNonLocked());
        return mapToAdminResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse verifyFarmer(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != UserRole.FARMER) {
            throw new BusinessException("Only farmers can be verified.");
        }

        user.setVerified(true);
        return mapToAdminResponse(userRepository.save(user));
    }

    // Helper to map Entity to DTO
    private AdminUserResponse mapToAdminResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getRole(),
                user.isVerified(),
                user.isAccountNonLocked(),
                user.getCreatedAt()
        );
    }

    // HELPERS
    private UUID getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new BusinessException("Authentication required");
        }

        // Cast the principal to your User entity
        User admin = (User) authentication.getPrincipal();

        // Double-check they are actually an ADMIN (Defense in depth)
        if (admin.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Access Denied: Admin privileges required.");
        }

        return admin.getId();
    }
}