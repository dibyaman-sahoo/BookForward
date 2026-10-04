package com.bookforward.service;

import com.bookforward.entity.OrderStatus;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Pure rules for order transitions, kept separate so they are trivially unit-testable. */
public final class OrderStateMachine {
    private OrderStateMachine() {}

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.HANDOVER, OrderStatus.CANCELLED),
            OrderStatus.HANDOVER, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED),
            OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** HANDOVER is declared by the seller, COMPLETED is confirmed by the buyer, either party may cancel. */
    public static boolean actorAllowed(OrderStatus to, boolean isBuyer, boolean isSeller) {
        return switch (to) {
            case HANDOVER -> isSeller;
            case COMPLETED -> isBuyer;
            case CANCELLED -> isBuyer || isSeller;
            default -> false;
        };
    }
}
