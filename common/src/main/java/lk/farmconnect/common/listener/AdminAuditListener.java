package lk.farmconnect.common.listener;

import lk.farmconnect.common.entity.AdminAuditLog;
import lk.farmconnect.common.event.AdminAuditEvent;
import lk.farmconnect.common.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditListener {

    private final AdminAuditLogRepository auditLogRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensures audit is saved even if main transaction rolls back
    public void handleAuditEvent(AdminAuditEvent event) {
        try {
            AdminAuditLog adminAuditLoglog = AdminAuditLog.builder()
                    .adminId(event.getAdminId())
                    .action(event.getAction())
                    .targetEntityId(event.getTargetEntityId())
                    .details(event.getDetails())
                    .build();

            auditLogRepository.save(adminAuditLoglog);
            log.info("Audit Log Saved: Admin {} performed {} on {}", event.getAdminId(), event.getAction(), event.getTargetEntityId());
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}