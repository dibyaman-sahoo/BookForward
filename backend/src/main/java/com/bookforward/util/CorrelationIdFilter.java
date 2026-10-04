package com.bookforward.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds a correlation id to every request for log tracing; echoed in the X-Correlation-Id header. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String incoming = req.getHeader("X-Correlation-Id");
        String cid = (incoming != null && incoming.matches("[A-Za-z0-9-]{8,64}")) ? incoming : UUID.randomUUID().toString();
        MDC.put("cid", cid);
        res.setHeader("X-Correlation-Id", cid);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("cid");
        }
    }
}
