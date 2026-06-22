package lk.farmconnect.chat.repository;

import lk.farmconnect.chat.entity.Conversation;
import lk.farmconnect.order.entity.Order;
import lk.farmconnect.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByOrder(Order order);

    boolean existsByOrder(Order order);

    // Find conversation between two specific users (useful for direct messaging later)
    Optional<Conversation> findByBuyerAndFarmer(User buyer, User farmer);
}
