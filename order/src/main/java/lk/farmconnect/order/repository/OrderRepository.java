package lk.farmconnect.order.repository;

import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.entity.OrderStatus;
import lk.farmconnect.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Fetch orders for a specific buyer, eagerly loading items to prevent N+1
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findByBuyerAndStatusIn(User buyer, java.util.List<OrderStatus> statuses, Pageable pageable);

    // Fetch orders for a specific farmer (Denormalized field makes this fast)
    @EntityGraph(attributePaths = {"items", "items.product", "buyer"})
    Page<Order> findByFarmerAndStatusIn(User farmer, java.util.List<OrderStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product", "buyer", "farmer"})
    Optional<Order> findById(UUID id);

    boolean existsByOrderNumber(String orderNumber);

    // Calculate total revenue for a farmer (Only COMPLETED orders)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.farmer.id = :farmerId AND o.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenue(@Param("farmerId") UUID farmerId);

    // Count orders by specific statuses
    @Query("SELECT COUNT(o) FROM Order o WHERE o.farmer.id = :farmerId AND o.status IN :statuses")
    long countOrdersByStatuses(@Param("farmerId") UUID farmerId, @Param("statuses") List<OrderStatus> statuses);

    // Fetch the 3 most recent orders for a buyer (For the dashboard preview)
    @EntityGraph(attributePaths = {"items", "items.product", "farmer"})
    @Query("SELECT o FROM Order o WHERE o.buyer.id = :buyerId ORDER BY o.createdAt DESC")
    Page<Order> findRecentOrdersByBuyer(@Param("buyerId") UUID buyerId, Pageable pageable);


    // Buyer Order History
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);
    Page<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(UUID buyerId, OrderStatus status, Pageable pageable);

    // Farmer Received Orders
    Page<Order> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId, Pageable pageable);
    Page<Order> findByFarmerIdAndStatusOrderByCreatedAtDesc(UUID farmerId, OrderStatus status, Pageable pageable);

}