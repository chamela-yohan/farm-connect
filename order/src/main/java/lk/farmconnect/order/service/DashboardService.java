package lk.farmconnect.order.service;

import lk.farmconnect.order.dto.BuyerDashboardSummary;
import lk.farmconnect.order.dto.FarmerDashboardSummary;
import lk.farmconnect.order.dto.OrderResponse;
import lk.farmconnect.order.entity.OrderStatus;
import lk.farmconnect.order.mapper.OrderMapper;
import lk.farmconnect.order.repository.OrderRepository;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    // CACHED: Runs DB query only once per 5 minutes (if using default cache config)
    @Cacheable(value = "farmerDashboard", key = "#farmerId")
    @Transactional(readOnly = true)
    public FarmerDashboardSummary getFarmerDashboard(UUID farmerId) {
        User farmer = userRepository.findById(farmerId).orElseThrow();

        BigDecimal revenue = orderRepository.calculateTotalRevenue(farmerId);

        long active = orderRepository.countOrdersByStatuses(farmerId, List.of(
                OrderStatus.ACCEPTED, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY
        ));

        long pending = orderRepository.countOrdersByStatuses(farmerId, List.of(OrderStatus.PENDING));
        long completed = orderRepository.countOrdersByStatuses(farmerId, List.of(OrderStatus.COMPLETED));

        return new FarmerDashboardSummary(
                revenue, active, pending, completed,
                farmer.getAverageRating() != null ? farmer.getAverageRating() : 0.0,
                farmer.getTotalReviews() != null ? farmer.getTotalReviews() : 0
        );
    }

    @Cacheable(value = "buyerDashboard", key = "#buyerId")
    @Transactional(readOnly = true)
    public BuyerDashboardSummary getBuyerDashboard(UUID buyerId) {
        long active = orderRepository.countOrdersByStatuses(buyerId, List.of(
                OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY
        ));

        long completed = orderRepository.countOrdersByStatuses(buyerId, List.of(OrderStatus.COMPLETED));

        // Fetch top 3 recent orders
        List<OrderResponse> recentOrders = orderRepository.findRecentOrdersByBuyer(buyerId, PageRequest.of(0, 3))
                .map(orderMapper::toOrderResponse)
                .getContent();

        return new BuyerDashboardSummary(active, completed, recentOrders);
    }
}