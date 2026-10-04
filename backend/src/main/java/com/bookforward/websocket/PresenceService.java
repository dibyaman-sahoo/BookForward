package com.bookforward.websocket;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Presence boundary: sessions are tracked separately from logical users. A Redis adapter can replace the in-memory one. */
public interface PresenceService {
    void connected(UUID userId, String sessionId);
    /** @return the user id if that user has no remaining sessions (i.e. went offline). */
    Optional<UUID> disconnected(String sessionId);
    Set<UUID> onlineUsers();
    boolean isOnline(UUID userId);
}
