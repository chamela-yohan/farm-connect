package lk.farmconnect.chat.controller;

import lk.farmconnect.chat.dto.MessageRequest;
import lk.farmconnect.chat.dto.MessageResponse;
import lk.farmconnect.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // Listens for messages sent to /app/chat.send/{conversationId}
    @MessageMapping("/chat.send/{conversationId}")
    public void sendMessage(
            @DestinationVariable UUID conversationId,
            @Valid @Payload MessageRequest request,
            Principal principal) {

        // Extract User ID from the Principal (set by our JWT Interceptor)
        UUID senderId = UUID.fromString(principal.getName());

        // Save to Database & Validate Permissions
        MessageResponse response = chatService.sendMessage(conversationId, senderId, request.content());

        // Broadcast to the specific chat room topic
        // The frontend will subscribe to "/topic/chat.{conversationId}" to receive this
        messagingTemplate.convertAndSend("/topic/chat." + conversationId, response);
    }
}