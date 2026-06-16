package lk.farmconnect.order.repository;

import lk.farmconnect.order.entity.Order;
import lk.farmconnect.order.entity.OrderStatus;
import lk.farmconnect.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}