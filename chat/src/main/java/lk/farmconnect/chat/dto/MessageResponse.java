package lk.farmconnect.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderName,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {}