package com.bookforward.security;

import com.bookforward.entity.UserStatus;
import com.bookforward.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Validates a bearer token against the database (status + token version = logout/revocation). */
@Component
@RequiredArgsConstructor
public class TokenAuthenticator {
    private final JwtService jwt;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Optional<UserPrincipal> authenticate(String token) {
        try {
            Claims c = jwt.parse(token);
            UUID id = UUID.fromString(c.getSubject());
            Integer ver = c.get("ver", Integer.class);
            return users.findById(id)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE && ver != null && u.getTokenVersion() == ver)
                    .map(u -> new UserPrincipal(u.getId(), u.getEmail(), new HashSet<>(u.getRoles())));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
