package lk.farmconnect.order.service;

import lk.farmconnect.common.service.EmailService;
import lk.farmconnect.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final EmailService emailService; // Injected from 'common'

    public void sendOrderAcceptedEmail(Order order) {
        Map<String, Object> context = new HashMap<>();
        context.put("buyerName", order.getBuyer().getName());
        context.put("orderNumber", order.getOrderNumber());
        context.put("totalAmount", order.getTotalAmount().toPlainString());
        context.put("farmerMobile", order.getFarmer().getMobileNumber());
        context.put("farmerName", order.getFarmer().getName());
        context.put("orderId", order.getId());

        emailService.sendHtmlEmail(
                order.getBuyer().getEmail(),
                "Order Accepted: " + order.getOrderNumber(),
                "email/order-accepted",
                context
        );
    }

    public void sendOrderRejectedEmail(Order order) {
        Map<String, Object> context = new HashMap<>();
        context.put("buyerName", order.getBuyer().getName());
        context.put("orderNumber", order.getOrderNumber());
        context.put("rejectionReason", order.getFarmerNotes() != null ? order.getFarmerNotes() : "No reason provided.");

        emailService.sendHtmlEmail(
                order.getBuyer().getEmail(),
                "Order Declined: " + order.getOrderNumber(),
                "email/order-rejected",
                context
        );
    }
}