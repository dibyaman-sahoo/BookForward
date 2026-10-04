package com.bookforward.security;

/** Fixed-window limiter boundary; the in-memory adapter can be swapped for a Redis-backed one. */
public interface RateLimiter {
    /** @return true if the call is allowed. */
    boolean tryAcquire(String key, int limitPerMinute);
}
