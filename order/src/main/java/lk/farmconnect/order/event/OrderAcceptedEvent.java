package lk.farmconnect.order.event;

import lk.farmconnect.order.entity.Order;
import org.springframework.context.ApplicationEvent;

public class OrderAcceptedEvent extends ApplicationEvent {
    private final Order order;

    public OrderAcceptedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}