package lk.farmconnect.order.event;

import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        Order order = event.getOrder();
        if (order.getBuyer().getEmail() != null) {
            emailService.sendOrderAcceptedEmail(order);
        }
    }

    @Async
    @EventListener
    public void handleOrderRejected(OrderRejectedEvent event) {
        Order order = event.getOrder();
        if (order.getBuyer().getEmail() != null) {
            emailService.sendOrderRejectedEmail(order);
        }
    }
}