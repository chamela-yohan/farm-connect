package lk.farmconnect.user.service;

import lk.farmconnect.common.event.AdminAuditEvent;
import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.UserRole; // Ensure you have this enum
import lk.farmconnect.user.dto.AdminUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher; // Injected for Audit Events

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToAdminResponse);
    }

    @Transactional
    public AdminUserResponse toggleUserStatus(UUID userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User admin = getCurrentAdmin();

        // Prevent admin from locking themselves out
        if (targetUser.getId().equals(admin.getId())) {
            throw new BusinessException("You cannot suspend your own account.");
        }

        boolean newStatus = !targetUser.isAccountNonLocked();
        targetUser.setAccountNonLocked(newStatus);
        userRepository.save(targetUser);

        // Publish Audit Event
        eventPublisher.publishEvent(new AdminAuditEvent(
                this,
                admin.getId(),
                newStatus ? "SUSPEND_USER" : "UNSUSPEND_USER",
                userId.toString(),
                "Account locked status changed to: " + newStatus
        ));

        return mapToAdminResponse(targetUser);
    }

    @Transactional
    public AdminUserResponse verifyFarmer(UUID userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (targetUser.getRole() != UserRole.FARMER) {
            throw new BusinessException("Only farmers can be verified.");
        }

        targetUser.setVerified(true);
        userRepository.save(targetUser);

        User admin = getCurrentAdmin();

        // Publish Audit Event
        eventPublisher.publishEvent(new AdminAuditEvent(
                this,
                admin.getId(),
                "VERIFY_FARMER",
                userId.toString(),
                "Farmer KYC approved by admin"
        ));

        return mapToAdminResponse(targetUser);
    }

    // Helper to get current admin from Security Context
    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            throw new BusinessException("Authentication required");
        }
        User admin = (User) auth.getPrincipal();
        if (admin.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Access Denied: Admin privileges required.");
        }
        return admin;
    }

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
}