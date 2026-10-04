package com.bookforward.websocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryPresenceService implements PresenceService {
    private final Map<String, UUID> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void connected(UUID userId, String sessionId) { sessionToUser.put(sessionId, userId); }

    @Override
    public Optional<UUID> disconnected(String sessionId) {
        UUID user = sessionToUser.remove(sessionId);
        if (user == null || sessionToUser.containsValue(user)) return Optional.empty();
        return Optional.of(user);
    }

    @Override
    public Set<UUID> onlineUsers() { return Set.copyOf(new HashSet<>(sessionToUser.values())); }

    @Override
    public boolean isOnline(UUID userId) { return sessionToUser.containsValue(userId); }
}
