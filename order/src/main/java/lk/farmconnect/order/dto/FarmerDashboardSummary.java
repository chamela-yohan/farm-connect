package lk.farmconnect.order.dto;

import java.math.BigDecimal;

public record FarmerDashboardSummary(
        BigDecimal totalRevenue,       // From COMPLETED orders
        long activeOrdersCount,        // ACCEPTED, READY_FOR_PICKUP, OUT_FOR_DELIVERY
        long pendingOrdersCount,       // PENDING
        long completedOrdersCount,     // COMPLETED
        double averageRating,          // From User entity
        int totalReviews               // From User entity
) {}