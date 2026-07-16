package lk.farmconnect.booking.service;

import lk.farmconnect.booking.dto.BookingResponse;
import lk.farmconnect.booking.dto.BookingStatusUpdateRequest;
import lk.farmconnect.booking.entity.Booking;
import lk.farmconnect.booking.entity.BookingStatus;
import lk.farmconnect.booking.mapper.BookingMapper;
import lk.farmconnect.booking.repository.BookingRepository;
import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.product.entity.Product;
import lk.farmconnect.product.entity.ProductType;
import lk.farmconnect.product.repository.ProductRepository;
import lk.farmconnect.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponse createBookingRequest(User buyer, UUID productId, LocalDate startDate, LocalDate endDate, Integer quantity, String notes) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));

        if (product.getProductType() != ProductType.RENTABLE && product.getProductType() != ProductType.SERVICE) {
            throw new BusinessException("This product is not available for booking.");
        }

        if (product.getFarmer().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot book your own product.");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Start date cannot be in the past.");
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessException("End date must be after start date.");
        }

        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Quantity must be at least 1.");
        }

        // 1. Get Available Units from JSONB attributes (Default to 1 if not set)
        Integer availableUnits = 1;
        if (product.getAttributes() != null && product.getAttributes().has("availableUnits")) {
            availableUnits = product.getAttributes().get("availableUnits").asInt();
        }

        // 2. Check Capacity (Prevent Overbooking)
        Integer currentlyBooked = bookingRepository.getTotalBookedQuantityForPeriod(productId, startDate, endDate);

        if (currentlyBooked + quantity > availableUnits) {
            throw new BusinessException(
                    String.format("Only %d unit(s) available for these dates. You requested %d.",
                            (availableUnits - currentlyBooked), quantity)
            );
        }

        // 3. Calculate Pricing dynamically from JSONB attributes
        BigDecimal rentalPerDay = product.getAttributes().get("rentalPricePerDay") != null
                ? new BigDecimal(product.getAttributes().get("rentalPricePerDay").asText())
                : product.getPrice();

        BigDecimal depositPerUnit = product.getAttributes().get("depositAmount") != null
                ? new BigDecimal(product.getAttributes().get("depositAmount").asText())
                : BigDecimal.ZERO;

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1; // Inclusive

        // Multiply by Quantity
        BigDecimal rentalAmount = rentalPerDay.multiply(BigDecimal.valueOf(days)).multiply(BigDecimal.valueOf(quantity));
        BigDecimal totalDeposit = depositPerUnit.multiply(BigDecimal.valueOf(quantity));
        BigDecimal totalAmount = rentalAmount.add(totalDeposit);

        Booking booking = Booking.builder()
                .product(product)
                .buyer(buyer)
                .farmer(product.getFarmer())
                .startDate(startDate)
                .endDate(endDate)
                .quantity(quantity) // Save the quantity
                .totalDays((int) days)
                .rentalAmount(rentalAmount)
                .depositAmount(totalDeposit) // Total deposit for all units
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .buyerNotes(notes)
                .build();

        Booking savedbooking =  bookingRepository.save(booking);
        return  bookingMapper.toResponse(savedbooking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getFarmerBookings(User farmer, String status, Pageable pageable) {
        Page<Booking> bookings;

        if (status != null && !status.isBlank()) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingRepository.findByFarmerIdAndStatus(farmer.getId(), bookingStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid booking status provided.");
            }
        } else {
            bookings = bookingRepository.findByFarmerId(farmer.getId(), pageable);
        }

        return bookings.map(bookingMapper::toResponse);
    }

    @Transactional
    public BookingResponse updateBookingStatus(UUID bookingId, BookingStatusUpdateRequest request, User currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));

        if (!booking.getFarmer().getId().equals(currentUser.getId())) {
            throw new BusinessException("Access Denied: You do not own this booking.");
        }

        BookingStatus oldStatus = booking.getStatus();

        if (oldStatus == BookingStatus.COMPLETED || oldStatus == BookingStatus.REJECTED || oldStatus == BookingStatus.CANCELLED) {
            throw new BusinessException("Cannot update a booking that is already in a terminal state.");
        }

        if (request.newStatus() == BookingStatus.ACCEPTED && oldStatus != BookingStatus.PENDING) {
            throw new BusinessException("Can only accept a PENDING booking.");
        }

        booking.setStatus(request.newStatus());

        if (request.notes() != null && !request.notes().isBlank()) {
            booking.setFarmerNotes(request.notes());
        }

        log.info("Farmer {} updated booking {} from {} to {}", currentUser.getId(), bookingId, oldStatus, request.newStatus());

        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getBuyerBookings(User buyer, String status, Pageable pageable) {
        Page<Booking> bookings;

        if (status != null && !status.isBlank()) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingRepository.findByBuyerIdAndStatus(buyer.getId(), bookingStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid booking status provided.");
            }
        } else {
            bookings = bookingRepository.findByBuyerId(buyer.getId(), pageable);
        }

        return bookings.map(bookingMapper::toResponse);
    }
}