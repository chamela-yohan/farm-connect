package lk.farmconnect.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class AdminAuditEvent extends ApplicationEvent {
    private final UUID adminId;
    private final String action;
    private final String targetEntityId;
    private final String details;

    public AdminAuditEvent(Object source, UUID adminId, String action, String targetEntityId, String details) {
        super(source);
        this.adminId = adminId;
        this.action = action;
        this.targetEntityId = targetEntityId;
        this.details = details;
    }

}