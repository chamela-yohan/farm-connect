package lk.farmconnect.chat.service;

import lk.farmconnect.chat.dto.ConversationSummaryResponse;
import lk.farmconnect.chat.dto.MessageResponse;
import lk.farmconnect.chat.entity.Conversation;
import lk.farmconnect.chat.entity.Message;
import lk.farmconnect.chat.mapper.MessageMapper;
import lk.farmconnect.chat.repository.ConversationRepository;
import lk.farmconnect.chat.repository.MessageRepository;
import lk.farmconnect.common.exception.BusinessException;
import lk.farmconnect.common.exception.ResourceNotFoundException;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    
    // REAL-TIME MESSAGE SENDING
    @Transactional
    public MessageResponse sendMessage(UUID conversationId, UUID senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        // CRITICAL SECURITY: Ensure sender is actually part of this conversation
        validateConversationAccess(conversation, senderId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .build();

        Message saved = messageRepository.save(message);
        log.info("Message saved in conversation {} by user {}", conversationId, senderId);

        return messageMapper.toResponse(saved);
    }

    
    // PAGINATED HISTORY (For REST API)
    
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatHistory(UUID conversationId, UUID userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateConversationAccess(conversation, userId);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(messageMapper::toResponse);
    }

    
    // READ RECEIPTS
    
    @Transactional
    public void markMessagesAsRead(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateConversationAccess(conversation, userId);

        // Marks all messages from the OTHER user as read
        messageRepository.markAsReadForConversation(conversationId, userId);
    }

    
    // PRIVATE HELPERS
    private void validateConversationAccess(Conversation conversation, UUID userId) {
        boolean isBuyer = conversation.getBuyer().getId().equals(userId);
        boolean isFarmer = conversation.getFarmer().getId().equals(userId);

        if (!isBuyer && !isFarmer) {
            throw new BusinessException("Access Denied: You are not authorized to view or participate in this conversation.");
        }
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getMyConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findAllByUser(userId);

        return conversations.stream().map(conv -> {
                    // Determine who the "other" user is
                    User otherUser = conv.getBuyer().getId().equals(userId) ? conv.getFarmer() : conv.getBuyer();

                    // Fetch the latest message preview
                    Message lastMsg = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conv.getId()).orElse(null);

                    // Fetch unread count
                    long unread = messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(conv.getId(), userId);

                    return new ConversationSummaryResponse(
                            conv.getId(),
                            conv.getOrder().getOrderNumber(),
                            otherUser.getId(),
                            otherUser.getName(),
                            otherUser.getProfilePictureUrl(),
                            lastMsg != null ? lastMsg.getContent() : "No messages yet",
                            lastMsg != null ? lastMsg.getCreatedAt() : conv.getCreatedAt(),
                            unread
                    );
                })
                // Sort by the most recent activity
                .sorted((a, b) -> b.lastMessageTime().compareTo(a.lastMessageTime()))
                .toList();
    }
}