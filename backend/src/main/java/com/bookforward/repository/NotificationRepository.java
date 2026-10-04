package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);
    long countByRecipientIdAndSeenFalse(UUID recipientId);
    boolean existsByRecipientIdAndNotificationTypeAndLinkAndSeenFalse(UUID recipientId, NotificationType type, String link);

    @Transactional
    @Modifying
    @Query("update Notification n set n.seen = true where n.recipient.id = :uid and n.seen = false")
    int markAllSeen(@Param("uid") UUID userId);
}
