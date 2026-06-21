package lk.farmconnect.order.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lk.farmconnect.order.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public SmtpEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendOrderAcceptedEmail(Order order) {
        try {
            Context context = new Context();
            context.setVariable("buyerName", order.getBuyer().getName());
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("totalAmount", order.getTotalAmount().toPlainString());
            context.setVariable("farmerName", order.getFarmer().getName());
            context.setVariable("farmerMobile", order.getFarmer().getMobileNumber());
            context.setVariable("invoiceUrl", order.getInvoiceUrl());

            String htmlContent = templateEngine.process("email/order-accepted", context);
            sendHtmlEmail(order.getBuyer().getEmail(), "Order Accepted: " + order.getOrderNumber(), htmlContent);

        } catch (Exception e) {
            log.error("Failed to send acceptance email for order {}", order.getOrderNumber(), e);
        }
    }

    @Override
    public void sendOrderRejectedEmail(Order order) {
        try {
            Context context = new Context();
            context.setVariable("buyerName", order.getBuyer().getName());
            context.setVariable("orderNumber", order.getOrderNumber());
            // Use the farmerNotes as the rejection reason
            context.setVariable("rejectionReason", order.getFarmerNotes() != null ? order.getFarmerNotes() : "No specific reason provided.");

            String htmlContent = templateEngine.process("email/order-rejected", context);
            sendHtmlEmail(order.getBuyer().getEmail(), "Order Declined: " + order.getOrderNumber(), htmlContent);

        } catch (Exception e) {
            log.error("Failed to send rejection email for order {}", order.getOrderNumber(), e);
        }
    }

    // Helper to send HTML emails
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("noreply@farmconnect.lk"); // Use a real domain in prod
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = isHtml

        mailSender.send(message);
        log.info("HTML email sent to {} with subject: {}", to, subject);
    }
}