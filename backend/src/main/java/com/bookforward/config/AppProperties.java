package com.bookforward.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookforward")
public record AppProperties(String env, String frontendOrigin, String wsAllowedOrigins, Jwt jwt, Storage storage,
                            Payment payment, RateLimit rateLimit, BootstrapAdmin bootstrapAdmin) {
    public record Jwt(String secret, long expiryMinutes) {}
    public record Storage(String provider, String localDir, long maxBytes) {}
    public record Payment(boolean enabled, String provider) {}
    public record RateLimit(int authPerMinute, int uploadPerMinute, int messagesPerMinute) {}
    public record BootstrapAdmin(String email, String password, String name) {}
}
