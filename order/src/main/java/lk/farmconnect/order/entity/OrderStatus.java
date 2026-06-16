package lk.farmconnect.order.entity;

public enum OrderStatus {
    PENDING,              // Buyer submitted request
    ACCEPTED,             // Farmer approved (Stock deducted here)
    REJECTED,             // Farmer declined
    READY_FOR_PICKUP,     // Farmer prepared the goods
    OUT_FOR_DELIVERY,     // Farmer is on the way
    DELIVERED,            // Buyer received goods
    COMPLETED,            // Transaction finalized (Review phase)
    CANCELLED             // Cancelled by either party
}