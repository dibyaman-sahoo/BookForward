package com.bookforward.dto;

import com.bookforward.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ChatDtos {
    private ChatDtos() {}

    public record MessageDto(UUID id, UUID conversationId, UUID senderId, String senderName, String content,
            MessageType type, Instant createdAt) {}
    public record ConversationDto(UUID id, UUID listingId, String listingTitle, TradeDtos.PartyDto other,
            boolean otherOnline, MessageDto lastMessage, long unread) {}
    public record StartConversationRequest(@NotNull UUID listingId) {}
    public record SendMessageRequest(@NotBlank @Size(max = 2000) String content) {}
    public record WsSendMessage(UUID conversationId, String content) {}
    public record WsTyping(UUID conversationId) {}
    public record PresenceDto(Set<UUID> online, Instant at) {}
}
