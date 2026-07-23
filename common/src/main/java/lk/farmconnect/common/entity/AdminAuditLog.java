package lk.farmconnect.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminId;

    @Column(nullable = false)
    private String action; // e.g., "SUSPEND_USER", "VERIFY_FARMER", "SOFT_DELETE_PRODUCT"

    private String targetEntityId; // The ID of the user/product being acted upon
    private String details; // Extra context (e.g., "Reason: Spam")

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}