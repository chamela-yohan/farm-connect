package lk.farmconnect.chat.controller;

import lk.farmconnect.chat.dto.ConversationSummaryResponse;
import lk.farmconnect.chat.dto.MessageResponse;
import lk.farmconnect.chat.service.ChatService;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat") // Base path for all chat endpoints
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // FETCH INBOX (List of all conversations)
    // Maps to: GET /api/v1/chat
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getMyConversations(
            @AuthenticationPrincipal User currentUser) {
        List<ConversationSummaryResponse> inbox = chatService.getMyConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(inbox));
    }

    // FETCH CHAT HISTORY (Paginated)
    // Maps to: GET /api/v1/chat/{conversationId}/messages
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getHistory(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> history = chatService.getChatHistory(conversationId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // MARK MESSAGES AS READ
    // Maps to: PUT /api/v1/chat/{conversationId}/read
    @PutMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User currentUser) {

        chatService.markMessagesAsRead(conversationId, currentUser.getId());
        // FIXED: Return null for data to match ApiResponse<Void>
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // GET CONVERSATION ID BY ORDER ID
    // Maps to: GET /api/v1/chat/order/{orderId}/conversation
    @GetMapping("/order/{orderId}/conversation")
    public ResponseEntity<ApiResponse<UUID>> getConversationByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {

        UUID conversationId = chatService.getConversationIdByOrderId(orderId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(conversationId));
    }


    // Get conversation ID for a specific booking
    @GetMapping("/booking/{bookingId}/conversation")
    public ResponseEntity<ApiResponse<UUID>> getConversationByBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User currentUser) {

        UUID conversationId = chatService.getConversationIdByBookingId(bookingId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(conversationId));
    }
}