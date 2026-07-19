package lk.farmconnect.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory message broker for these destinations
        // /topic is used for broadcasting messages to a chat room
        config.enableSimpleBroker("/topic");

        // Prefix for messages sent from client to server (e.g., /app/chat.send)
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for sending messages to a specific user (e.g., /user/queue/notifications)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                // Split the comma-separated string into an array
                .setAllowedOriginPatterns(allowedOrigins.split(","))
        // SockJS fallback ensures WebSockets work even if strict corporate firewalls block them
          .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register the JWT Interceptor to secure the inbound channel
        registration.interceptors(webSocketAuthInterceptor);
    }
}