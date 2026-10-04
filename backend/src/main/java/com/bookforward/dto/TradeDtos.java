package com.bookforward.dto;

import com.bookforward.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class TradeDtos {
    private TradeDtos() {}

    public record CreateRequestDto(@NotNull UUID listingId, @Size(max = 500) String message) {}
    public record RequestStatusUpdate(@NotNull RequestStatus status, @Size(max = 500) String reason) {}
    public record OrderStatusUpdate(@NotNull OrderStatus status, @Size(max = 500) String reason) {}
    public record PartyDto(UUID id, String name) {}
    public record RequestDto(UUID id, UUID listingId, String listingTitle, String coverUrl, PartyDto buyer, PartyDto seller,
            RequestStatus status, String message, UUID orderId, Instant createdAt) {}
    public record HistoryDto(String oldStatus, String newStatus, String actor, String reason, Instant at) {}
    public record OrderDto(UUID id, UUID listingId, String listingTitle, String coverUrl, PartyDto buyer, PartyDto seller,
            OrderStatus status, BigDecimal totalAmount, String currency, boolean reviewed, List<HistoryDto> history,
            Instant createdAt) {}
    public record PaymentDto(UUID id, String provider, String reference, PaymentStatus status, BigDecimal amount, String currency) {}
}
