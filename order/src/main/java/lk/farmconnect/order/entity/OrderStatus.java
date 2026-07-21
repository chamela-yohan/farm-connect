package lk.farmconnect.order.entity;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    PREPARING,
    OUT_FOR_DELIVERY,
    READY_FOR_PICKUP,
    DELIVERED,
    COMPLETED,
    REJECTED,
    CANCELLED
}