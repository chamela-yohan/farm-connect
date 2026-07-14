package lk.farmconnect.booking.entity;

public enum BookingStatus {
    PENDING,      // Buyer requested, waiting for farmer approval
    ACCEPTED,     // Farmer approved, dates are now locked
    REJECTED,     // Farmer declined the request
    ACTIVE,       // Current rental period has started
    COMPLETED,    // Rental period ended successfully
    CANCELLED     // Cancelled by buyer or farmer before/during rental
}