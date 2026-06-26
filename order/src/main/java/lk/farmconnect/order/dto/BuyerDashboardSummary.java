package lk.farmconnect.order.dto;

import java.util.List;

public record BuyerDashboardSummary(
        long activeOrdersCount,
        long completedOrdersCount,
        List<OrderResponse> recentOrders // The 3 most recent orders for the UI
) {}