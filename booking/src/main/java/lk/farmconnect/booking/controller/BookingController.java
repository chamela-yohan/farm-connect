package lk.farmconnect.booking.controller;


import jakarta.validation.Valid;
import lk.farmconnect.booking.dto.BookingResponse;
import lk.farmconnect.booking.dto.BookingStatusUpdateRequest;
import lk.farmconnect.booking.entity.Booking;
import lk.farmconnect.booking.entity.BookingStatus;
import lk.farmconnect.booking.service.BookingService;
import lk.farmconnect.common.response.ApiResponse;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal User buyer,
            @RequestParam UUID productId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String notes) {

        BookingResponse booking = bookingService.createBookingRequest(buyer, productId, startDate, endDate, quantity, notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(booking, "Booking request sent successfully"));
    }

    @GetMapping("/farmer")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getFarmerBookings(
            @AuthenticationPrincipal User farmer,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<BookingResponse> bookings = bookingService.getFarmerBookings(farmer, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BookingStatusUpdateRequest request,
            @AuthenticationPrincipal User farmer) {

        BookingResponse updatedBooking = bookingService.updateBookingStatus(id, request, farmer);
        return ResponseEntity.ok(ApiResponse.success(updatedBooking, "Booking status updated successfully"));
    }


    @GetMapping("/buyer")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBuyerBookings(
            @AuthenticationPrincipal User buyer,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<BookingResponse> bookings = bookingService.getBuyerBookings(buyer, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}