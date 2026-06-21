package lk.farmconnect.order.service;

import lk.farmconnect.order.entity.Order;

public interface EmailService {
    void sendOrderAcceptedEmail(Order order);
    void sendOrderRejectedEmail(Order order);
}