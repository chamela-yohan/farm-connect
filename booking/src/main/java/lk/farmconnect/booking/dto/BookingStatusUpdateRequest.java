package lk.farmconnect.booking.dto;

import jakarta.validation.constraints.NotNull;
import lk.farmconnect.booking.entity.BookingStatus;

public record BookingStatusUpdateRequest(
        @NotNull(message = "New status is required")
        BookingStatus newStatus,

        String notes
) {}