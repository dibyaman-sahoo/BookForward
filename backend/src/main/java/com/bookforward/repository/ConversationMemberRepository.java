package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {
    List<ConversationMember> findByUserId(UUID userId);
    List<ConversationMember> findByConversationId(UUID conversationId);
    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);
}
