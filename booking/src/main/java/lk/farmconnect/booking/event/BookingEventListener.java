package lk.farmconnect.booking.event;

import lk.farmconnect.booking.entity.BookingStatus;
import lk.farmconnect.booking.service.BookingNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final BookingNotificationService bookingNotificationService;

    @Async
    @EventListener
    public void handleBookingStatusUpdate(BookingStatusUpdatedEvent event) {
        BookingNotificationPayload payload = event.getPayload();
        BookingStatus status = payload.status();

        System.out.println(" Try to notify changed Booking Status: " + status);

        try {
            if (status == BookingStatus.PENDING) {
                if (payload.farmerEmail() != null) {
                    bookingNotificationService.sendBookingRequestedEmail(payload);
                }
            } else if (status == BookingStatus.ACCEPTED) {
                if (payload.buyerEmail() != null) {
                    bookingNotificationService.sendBookingAcceptedEmail(payload);
                }
            } else if (status == BookingStatus.REJECTED || status == BookingStatus.CANCELLED) {
                if (payload.buyerEmail() != null) {
                    bookingNotificationService.sendBookingRejectedEmail(payload);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process booking email notification for status {}", status, e);
        }
    }
}