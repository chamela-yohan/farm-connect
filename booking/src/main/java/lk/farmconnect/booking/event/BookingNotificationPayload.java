package lk.farmconnect.booking.event;

import lk.farmconnect.booking.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingNotificationPayload(
        UUID bookingId,
        BookingStatus status,
        String buyerEmail,
        String buyerName,
        String farmerEmail,
        String farmerName,
        String farmerMobile,
        String productTitle,
        LocalDate startDate,
        LocalDate endDate,
        Integer totalDays,
        Integer quantity,
        BigDecimal totalAmount,
        String buyerNotes,
        String farmerNotes
) {}