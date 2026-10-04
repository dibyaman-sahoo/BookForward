package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);
    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(UUID conversationId, UUID senderId, Instant after);
    long countByConversationIdAndSenderIdNot(UUID conversationId, UUID senderId);
}
