package com.bookforward.security;

import com.bookforward.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rate controls for auth and upload endpoints (messages are limited in ChatService). */
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter limiter;
    private final AppProperties props;
    private final ObjectMapper mapper;

    public RateLimitFilter(RateLimiter limiter, AppProperties props, ObjectMapper mapper) {
        this.limiter = limiter;
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        boolean post = "POST".equals(req.getMethod());
        boolean allowed = true;
        if (post && (path.equals("/api/auth/login") || path.equals("/api/auth/register"))) {
            allowed = limiter.tryAcquire("auth:" + req.getRemoteAddr(), props.rateLimit().authPerMinute());
        } else if (post && path.matches("/api/listings/[^/]+/images")) {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            String who = (a != null && a.getPrincipal() instanceof UserPrincipal p) ? p.getName() : req.getRemoteAddr();
            allowed = limiter.tryAcquire("upload:" + who, props.rateLimit().uploadPerMinute());
        }
        if (!allowed) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json");
            mapper.writeValue(res.getOutputStream(), Map.of("timestamp", Instant.now().toString(), "status", 429,
                    "code", "RATE_LIMITED", "message", "Too many requests. Please wait a minute and try again.",
                    "path", path, "fieldErrors", List.of()));
            return;
        }
        chain.doFilter(req, res);
    }
}
