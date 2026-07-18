package lk.farmconnect.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID conversationId,
        String orderNumber,
        String bookingId,
        UUID otherUserId,
        String otherUserName,
        String otherUserProfilePicture,
        String lastMessagePreview,
        LocalDateTime lastMessageTime,
        long unreadCount
) {}