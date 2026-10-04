package com.bookforward.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class InMemoryRateLimiter implements RateLimiter {
    private record Window(long minute, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limitPerMinute) {
        long minute = System.currentTimeMillis() / 60_000;
        Window w = windows.compute(key, (k, old) -> (old == null || old.minute() != minute) ? new Window(minute, new AtomicInteger()) : old);
        return w.count().incrementAndGet() <= limitPerMinute;
    }

    @Scheduled(fixedDelay = 300_000)
    void evictStale() {
        long minute = System.currentTimeMillis() / 60_000;
        windows.values().removeIf(w -> w.minute() < minute);
    }
}
