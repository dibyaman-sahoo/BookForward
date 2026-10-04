package com.bookforward.controller;

import com.bookforward.dto.*;
import com.bookforward.dto.AuthDtos.*;
import com.bookforward.dto.ListingDtos.*;
import com.bookforward.dto.TradeDtos.*;
import com.bookforward.dto.ChatDtos.*;
import com.bookforward.dto.EngagementDtos.*;
import com.bookforward.dto.AdminDtos.*;
import com.bookforward.entity.*;
import com.bookforward.security.UserPrincipal;
import com.bookforward.service.*;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bookforward.notification.NotificationService;
import com.bookforward.websocket.PresenceService;

@RestController
@RequiredArgsConstructor
public class MessagingController {
    private final ChatService chat;
    private final NotificationService notifications;
    private final PresenceService presence;

    @GetMapping("/api/conversations")
    public List<ConversationDto> conversations(@AuthenticationPrincipal UserPrincipal p) { return chat.list(p.getId()); }

    @PostMapping("/api/conversations")
    public ConversationDto start(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody StartConversationRequest r) { return chat.start(p.getId(), r.listingId()); }

    @GetMapping("/api/conversations/{id}/messages")
    public PageResponse<MessageDto> messages(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) { return chat.history(id, p.getId(), page, size); }

    /** REST fallback for sending when the WebSocket is down. */
    @PostMapping("/api/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody SendMessageRequest r) { return chat.post(id, p.getId(), r.content(), MessageType.TEXT); }

    @PostMapping("/api/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { chat.markRead(id, p.getId()); }

    @GetMapping("/api/presence")
    public PresenceDto presence() { return new PresenceDto(presence.onlineUsers(), java.time.Instant.now()); }

    @GetMapping("/api/notifications")
    public PageResponse<NotificationDto> notifications(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return notifications.list(p.getId(), page, size); }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal UserPrincipal p) { return Map.of("unread", notifications.unreadCount(p.getId())); }

    @PatchMapping("/api/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { notifications.markRead(p.getId(), id); }

    @PostMapping("/api/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAll(@AuthenticationPrincipal UserPrincipal p) { notifications.markAllRead(p.getId()); }
}
