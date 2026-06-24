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
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // FETCH CHAT HISTORY (Paginated)
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
    @PutMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User currentUser) {

        chatService.markMessagesAsRead(conversationId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read"));
    }
    // FETCH INBOX (List of all conversations)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getMyConversations(
            @AuthenticationPrincipal User currentUser) {

        List<ConversationSummaryResponse> inbox = chatService.getMyConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(inbox));
    }
}