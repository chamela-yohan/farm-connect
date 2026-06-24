package lk.farmconnect.chat.repository;

import lk.farmconnect.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversation.id = :conversationId " +
            "AND m.sender.id != :userId AND m.isRead = false")
    void markAsReadForConversation(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    // 'SenderIdNot' to explicitly compare the ID property of the User entity
    long countByConversationIdAndIsReadFalseAndSenderIdNot(UUID conversationId, UUID senderId);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}