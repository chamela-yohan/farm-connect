package lk.farmconnect.order.event;

import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.service.OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final OrderNotificationService orderNotificationService;

    @Async
    @EventListener
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        Order order = event.getOrder();
        if (order.getBuyer().getEmail() != null) {
            orderNotificationService.sendOrderAcceptedEmail(order);
        }
    }

    @Async
    @EventListener
    public void handleOrderRejected(OrderRejectedEvent event) {
        Order order = event.getOrder();
        if (order.getBuyer().getEmail() != null) {
            orderNotificationService.sendOrderRejectedEmail(order);
        }
    }
}