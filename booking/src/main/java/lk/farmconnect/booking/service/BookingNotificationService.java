package lk.farmconnect.booking.service;

import lk.farmconnect.booking.event.BookingNotificationPayload;
import lk.farmconnect.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingNotificationService {

    private final EmailService emailService;

    public void sendBookingRequestedEmail(BookingNotificationPayload payload) {
        Map<String, Object> context = new HashMap<>();
        context.put("farmerName", payload.farmerName());
        context.put("buyerName", payload.buyerName());
        context.put("productTitle", payload.productTitle());
        context.put("startDate", payload.startDate().toString());
        context.put("endDate", payload.endDate().toString());
        context.put("totalDays", payload.totalDays());
        context.put("quantity", payload.quantity());
        context.put("totalAmount", payload.totalAmount().toPlainString());
        context.put("buyerNotes", payload.buyerNotes());

        emailService.sendHtmlEmail(payload.farmerEmail(), "New Booking Request: " + payload.productTitle(), "email/booking-requested", context);
    }

    public void sendBookingAcceptedEmail(BookingNotificationPayload payload) {
        Map<String, Object> context = new HashMap<>();
        context.put("buyerName", payload.buyerName());
        context.put("productTitle", payload.productTitle());
        context.put("startDate", payload.startDate().toString());
        context.put("endDate", payload.endDate().toString());
        context.put("farmerName", payload.farmerName());
        context.put("farmerMobile", payload.farmerMobile());
        context.put("totalAmount", payload.totalAmount().toPlainString());
        context.put("farmerNotes", payload.farmerNotes());

        emailService.sendHtmlEmail(payload.buyerEmail(), "Booking Confirmed: " + payload.productTitle(), "email/booking-accepted", context);
    }

    public void sendBookingRejectedEmail(BookingNotificationPayload payload) {
        Map<String, Object> context = new HashMap<>();
        context.put("buyerName", payload.buyerName());
        context.put("productTitle", payload.productTitle());
        context.put("startDate", payload.startDate().toString());
        context.put("endDate", payload.endDate().toString());
        context.put("rejectionReason", payload.farmerNotes() != null ? payload.farmerNotes() : "No specific reason provided.");

        emailService.sendHtmlEmail(payload.buyerEmail(), "Booking Declined: " + payload.productTitle(), "email/booking-rejected", context);
    }
}