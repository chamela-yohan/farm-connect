package lk.farmconnect.chat.event;

import lk.farmconnect.chat.entity.Conversation;
import lk.farmconnect.chat.repository.ConversationRepository;
import lk.farmconnect.order.event.OrderAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAcceptedChatListener {

    private final ConversationRepository conversationRepository;

    // Synchronous Listener: Ensures the chat room exists immediately after acceptance
    @EventListener
    @Transactional
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        var order = event.getOrder();

        // Prevent duplicate conversations if the event is fired multiple times
        if (!conversationRepository.existsByOrder(order)) {
            Conversation conversation = Conversation.builder()
                    .order(order)
                    .buyer(order.getBuyer())
                    .farmer(order.getFarmer())
                    .build();

            conversationRepository.save(conversation);
            log.info("Auto-created chat conversation for Order: {}", order.getOrderNumber());
        }
    }
}