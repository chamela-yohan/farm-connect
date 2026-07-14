package lk.farmconnect.booking.controller;


import lk.farmconnect.booking.entity.Booking;
import lk.farmconnect.booking.service.BookingService;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(
            @AuthenticationPrincipal User buyer,
            @RequestParam UUID productId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String notes) {

        Booking booking = bookingService.createBookingRequest(buyer, productId, startDate, endDate, quantity, notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(booking, "Booking request sent successfully"));
    }
}