package lk.farmconnect.order.event;

import lk.farmconnect.order.entity.Order;
import org.springframework.context.ApplicationEvent;

public class OrderRejectedEvent extends ApplicationEvent {
    private final Order order;

    public OrderRejectedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}