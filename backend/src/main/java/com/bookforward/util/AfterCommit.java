package com.bookforward.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Runs side effects (e.g. WebSocket pushes) only once the surrounding transaction has committed. */
public final class AfterCommit {
    private AfterCommit() {}

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }
}
