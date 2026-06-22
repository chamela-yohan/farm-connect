package lk.farmconnect.chat.config;

import lk.farmconnect.auth.service.JwtService;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            // Check for both uppercase and lowercase "Authorization"
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization"); // Fallback
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket connection rejected: Missing or invalid Authorization header");
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                String email = jwtService.extractUsername(token);
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Principal principal = () -> user.getId().toString();
                accessor.setUser(principal);

                log.info("WebSocket connection established for user: {}", email);

            } catch (Exception e) {
                log.error("WebSocket authentication failed: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid or expired JWT token");
            }
        }

        return message;
    }
}