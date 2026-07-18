package lk.farmconnect.chat.event;

import lk.farmconnect.booking.entity.Booking;
import lk.farmconnect.booking.entity.BookingStatus;
import lk.farmconnect.booking.event.BookingStatusUpdatedEvent;
import lk.farmconnect.booking.repository.BookingRepository;
import lk.farmconnect.chat.entity.Conversation;
import lk.farmconnect.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingAcceptedChatListener {

    private final ConversationRepository conversationRepository;
    private final BookingRepository bookingRepository;

    @EventListener
    @Transactional
    public void handleBookingAccepted(BookingStatusUpdatedEvent event) {
        // Only create chat when the booking is officially ACCEPTED
        if (event.getPayload().status() == BookingStatus.ACCEPTED) {
            UUID bookingId = event.getPayload().bookingId();

            bookingRepository.findById(bookingId).ifPresent(booking -> {
                if (!conversationRepository.existsByBooking(booking)) {
                    Conversation conversation = Conversation.builder()
                            .booking(booking)
                            .buyer(booking.getBuyer())
                            .farmer(booking.getFarmer())
                            .build();

                    conversationRepository.save(conversation);
                    log.info("Auto-created chat conversation for Booking: {}", booking.getId());
                }
            });
        }
    }
}