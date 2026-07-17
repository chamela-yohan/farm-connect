package lk.farmconnect.booking.event;

import org.springframework.context.ApplicationEvent;

public class BookingStatusUpdatedEvent extends ApplicationEvent {
    private final BookingNotificationPayload payload;

    public BookingStatusUpdatedEvent(Object source, BookingNotificationPayload payload) {
        super(source);
        this.payload = payload;
    }

    public BookingNotificationPayload getPayload() {
        return payload;
    }
}