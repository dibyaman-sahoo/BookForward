package com.bookforward.websocket;

import com.bookforward.dto.ChatDtos.PresenceDto;
import com.bookforward.repository.UserRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Broadcasts the complete online set on every change so clients converge even if they missed an event. */
@Component
@RequiredArgsConstructor
public class PresenceEventListener {
    private final PresenceService presence;
    private final SimpMessagingTemplate messaging;
    private final UserRepository users;

    @EventListener
    public void onConnected(SessionConnectedEvent e) {
        Principal user = e.getUser();
        if (user == null) return;
        String sid = StompHeaderAccessor.wrap(e.getMessage()).getSessionId();
        presence.connected(UUID.fromString(user.getName()), sid);
        broadcast();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent e) {
        presence.disconnected(e.getSessionId()).ifPresent(uid -> users.touchLastSeen(uid, Instant.now()));
        broadcast();
    }

    private void broadcast() {
        messaging.convertAndSend("/topic/presence", new PresenceDto(presence.onlineUsers(), Instant.now()));
    }
}
