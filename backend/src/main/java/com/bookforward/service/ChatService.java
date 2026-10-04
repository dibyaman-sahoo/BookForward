package com.bookforward.service;

import com.bookforward.config.AppProperties;
import com.bookforward.dto.ChatDtos.*;
import com.bookforward.dto.PageResponse;
import com.bookforward.dto.TradeDtos.PartyDto;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.notification.NotificationService;
import com.bookforward.repository.*;
import com.bookforward.security.RateLimiter;
import com.bookforward.util.AfterCommit;
import com.bookforward.websocket.PresenceService;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final ListingRepository listings;
    private final UserRepository users;
    private final NotificationService notifications;
    private final SimpMessagingTemplate messaging;
    private final PresenceService presence;
    private final RateLimiter limiter;
    private final AppProperties props;

    @Transactional
    public ConversationDto start(UUID userId, UUID listingId) {
        Listing l = listings.findById(listingId).orElseThrow(() -> ApiException.notFound("Listing"));
        UUID sellerId = l.getSeller().getId();
        if (sellerId.equals(userId)) {
            throw ApiException.badRequest("OWN_LISTING", "You cannot message yourself about your own listing");
        }
        if (l.getStatus() == ListingStatus.DRAFT || l.getStatus() == ListingStatus.REMOVED) {
            throw ApiException.notFound("Listing");
        }
        return toDto(getOrCreate(l, userId, sellerId), userId);
    }

    /** One conversation per (listing, pair of users). Also used by the trade flow. */
    @Transactional
    public Conversation getOrCreate(Listing listing, UUID a, UUID b) {
        String key = listing.getId() + ":" + (a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a);
        return conversations.findByParticipantKey(key).orElseGet(() -> {
            try {
                Conversation c = new Conversation();
                c.setListing(listing);
                c.setParticipantKey(key);
                c = conversations.saveAndFlush(c);
                for (UUID uid : List.of(a, b)) {
                    ConversationMember m = new ConversationMember();
                    m.setConversation(c);
                    m.setUser(users.getReferenceById(uid));
                    m.setLastReadAt(Instant.now());
                    members.save(m);
                }
                return c;
            } catch (DataIntegrityViolationException e) {
                return conversations.findByParticipantKey(key).orElseThrow();
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> list(UUID userId) {
        return members.findByUserId(userId).stream().map(ConversationMember::getConversation)
                .map(c -> toDto(c, userId))
                .sorted(Comparator.comparing((ConversationDto d) -> d.lastMessage() == null ? Instant.EPOCH : d.lastMessage().createdAt()).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageDto> history(UUID convId, UUID userId, int page, int size) {
        requireMember(convId, userId);
        Page<Message> p = messages.findByConversationIdOrderByCreatedAtDesc(convId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.of(p, p.getContent().stream().map(this::toDto).toList());
    }

    @Transactional
    public MessageDto post(UUID convId, UUID senderId, String rawContent, MessageType type) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty() || content.length() > 2000) {
            throw ApiException.badRequest("INVALID_MESSAGE", "Message must be between 1 and 2000 characters");
        }
        if (!limiter.tryAcquire("msg:" + senderId, props.rateLimit().messagesPerMinute())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "You are sending messages too quickly");
        }
        ConversationMember me = requireMember(convId, senderId);
        Conversation c = me.getConversation();
        Message m = new Message();
        m.setConversation(c);
        m.setSender(me.getUser());
        m.setContent(content);
        m.setMessageType(type);
        m = messages.save(m);
        me.setLastReadAt(Instant.now());
        MessageDto dto = toDto(m);

        List<ConversationMember> all = members.findByConversationId(convId);
        for (ConversationMember other : all) {
            if (!other.getUser().getId().equals(senderId)) {
                String title = "New message from " + me.getUser().getDisplayName();
                notifications.notify(other.getUser(), NotificationType.NEW_MESSAGE, title, preview(content), "#/messages?c=" + convId);
            }
        }
        List<String> recipients = all.stream().map(x -> x.getUser().getId().toString()).toList();
        AfterCommit.run(() -> recipients.forEach(r -> messaging.convertAndSendToUser(r, "/queue/messages", dto)));
        return dto;
    }

    @Transactional
    public void markRead(UUID convId, UUID userId) {
        requireMember(convId, userId).setLastReadAt(Instant.now());
    }

    /** Typing indicators are ephemeral: never persisted. */
    @Transactional(readOnly = true)
    public void typing(UUID convId, UUID userId) {
        requireMember(convId, userId);
        for (ConversationMember m : members.findByConversationId(convId)) {
            if (!m.getUser().getId().equals(userId)) {
                messaging.convertAndSendToUser(m.getUser().getId().toString(), "/queue/typing",
                        Map.of("conversationId", convId, "userId", userId));
            }
        }
    }

    private ConversationMember requireMember(UUID convId, UUID userId) {
        return members.findByConversationIdAndUserId(convId, userId).orElseThrow(() -> ApiException.notFound("Conversation"));
    }

    private ConversationDto toDto(Conversation c, UUID viewer) {
        ConversationMember other = members.findByConversationId(c.getId()).stream()
                .filter(m -> !m.getUser().getId().equals(viewer)).findFirst().orElseThrow();
        ConversationMember mine = members.findByConversationIdAndUserId(c.getId(), viewer).orElseThrow();
        MessageDto last = messages.findFirstByConversationIdOrderByCreatedAtDesc(c.getId()).map(this::toDto).orElse(null);
        long unread = mine.getLastReadAt() == null
                ? messages.countByConversationIdAndSenderIdNot(c.getId(), viewer)
                : messages.countByConversationIdAndSenderIdNotAndCreatedAtAfter(c.getId(), viewer, mine.getLastReadAt());
        Listing l = c.getListing();
        return new ConversationDto(c.getId(), l == null ? null : l.getId(), l == null ? null : l.getTitle(),
                new PartyDto(other.getUser().getId(), other.getUser().getDisplayName()),
                presence.isOnline(other.getUser().getId()), last, unread);
    }

    private MessageDto toDto(Message m) {
        return new MessageDto(m.getId(), m.getConversation().getId(), m.getSender().getId(), m.getSender().getDisplayName(),
                m.getContent(), m.getMessageType(), m.getCreatedAt());
    }

    private static String preview(String s) { return s.length() > 120 ? s.substring(0, 117) + "..." : s; }
}
