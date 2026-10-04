package com.bookforward.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Not a @Component on purpose: it is registered only inside the security filter chain. */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final TokenAuthenticator authenticator;

    public JwtAuthFilter(TokenAuthenticator authenticator) { this.authenticator = authenticator; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authenticator.authenticate(header.substring(7)).ifPresent(p ->
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(p, null, p.authorities())));
        }
        chain.doFilter(req, res);
    }
}
