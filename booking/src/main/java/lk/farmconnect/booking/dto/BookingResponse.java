package lk.farmconnect.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,

        // Product Details
        UUID productId,
        String productTitle,

        // Buyer Details
        UUID buyerId,
        String buyerName,
        String buyerMobile,

        // Booking Details
        LocalDate startDate,
        LocalDate endDate,
        Integer totalDays,
        Integer quantity,

        // Financials
        BigDecimal rentalAmount,
        BigDecimal depositAmount,
        BigDecimal totalAmount,

        // Status & Notes
        String status,
        String buyerNotes,
        String farmerNotes,

        LocalDateTime createdAt
) {}