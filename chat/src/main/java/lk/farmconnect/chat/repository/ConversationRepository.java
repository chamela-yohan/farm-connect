package lk.farmconnect.chat.repository;

import lk.farmconnect.booking.entity.Booking;
import lk.farmconnect.chat.entity.Conversation;
import lk.farmconnect.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByOrder(Order order);
    boolean existsByOrder(Order order);

    Optional<Conversation> findByBooking(Booking booking);
    boolean existsByBooking(Booking booking);

    Optional<Conversation> findByBuyerIdAndFarmerId(UUID buyerId, UUID farmerId);

    @Query("SELECT c FROM Conversation c " +
            "LEFT JOIN FETCH c.order " +
            "LEFT JOIN FETCH c.booking " +
            "LEFT JOIN FETCH c.buyer " +
            "LEFT JOIN FETCH c.farmer " +
            "WHERE c.buyer.id = :userId OR c.farmer.id = :userId")
    List<Conversation> findAllByUser(@Param("userId") UUID userId);
}