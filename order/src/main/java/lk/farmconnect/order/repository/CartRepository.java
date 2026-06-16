package lk.farmconnect.order.repository;

import lk.farmconnect.order.entity.Cart;
import lk.farmconnect.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    // Eagerly load items and product details for the cart view
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByBuyer(User buyer);
}