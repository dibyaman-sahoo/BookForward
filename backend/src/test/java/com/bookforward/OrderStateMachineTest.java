package com.bookforward;

import static org.junit.jupiter.api.Assertions.*;

import com.bookforward.entity.OrderStatus;
import com.bookforward.service.OrderStateMachine;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {
    @Test
    void allowsForwardTransitions() {
        assertTrue(OrderStateMachine.canTransition(OrderStatus.CONFIRMED, OrderStatus.HANDOVER));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.HANDOVER, OrderStatus.COMPLETED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
    }

    @Test
    void rejectsSkippingAndTerminalStates() {
        assertFalse(OrderStateMachine.canTransition(OrderStatus.CONFIRMED, OrderStatus.COMPLETED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.HANDOVER));
    }

    @Test
    void enforcesWhoMayAct() {
        assertTrue(OrderStateMachine.actorAllowed(OrderStatus.HANDOVER, false, true));
        assertFalse(OrderStateMachine.actorAllowed(OrderStatus.HANDOVER, true, false));
        assertTrue(OrderStateMachine.actorAllowed(OrderStatus.COMPLETED, true, false));
        assertFalse(OrderStateMachine.actorAllowed(OrderStatus.COMPLETED, false, true));
        assertTrue(OrderStateMachine.actorAllowed(OrderStatus.CANCELLED, true, false));
    }
}
