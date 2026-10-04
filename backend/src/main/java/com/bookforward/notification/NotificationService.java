package com.bookforward.notification;

import com.bookforward.dto.EngagementDtos.NotificationDto;
import com.bookforward.dto.PageResponse;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.repository.NotificationRepository;
import com.bookforward.util.AfterCommit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notifications are persisted first; the WebSocket push is only a delivery optimisation. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository repo;
    private final SimpMessagingTemplate messaging;

    @Transactional
    public void notify(User recipient, NotificationType type, String title, String body, String link) {
        // collapse repeated unread-message notifications for the same conversation
        if (type == NotificationType.NEW_MESSAGE
                && repo.existsByRecipientIdAndNotificationTypeAndLinkAndSeenFalse(recipient.getId(), type, link)) {
            return;
        }
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        NotificationDto dto = toDto(repo.save(n));
        UUID rid = recipient.getId();
        AfterCommit.run(() -> {
            try {
                messaging.convertAndSendToUser(rid.toString(), "/queue/notifications", dto);
            } catch (RuntimeException e) {
                log.debug("Live notification push failed (will be fetched later): {}", e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(UUID userId, int page, int size) {
        Page<Notification> p = repo.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)));
        return PageResponse.of(p, p.getContent().stream().map(this::toDto).toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) { return repo.countByRecipientIdAndSeenFalse(userId); }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification n = repo.findById(id).filter(x -> x.getRecipient().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Notification"));
        n.setSeen(true);
    }

    @Transactional
    public int markAllRead(UUID userId) { return repo.markAllSeen(userId); }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getNotificationType(), n.getTitle(), n.getBody(), n.getLink(), n.isSeen(), n.getCreatedAt());
    }
}
