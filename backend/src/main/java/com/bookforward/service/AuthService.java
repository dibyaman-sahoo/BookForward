package com.bookforward.service;

import com.bookforward.admin.AuditService;
import com.bookforward.dto.AuthDtos.*;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.repository.*;
import com.bookforward.security.JwtService;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(r.displayName().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRoles(new java.util.HashSet<>(Set.of(Role.USER)));
        u = users.save(u);
        Profile p = new Profile();
        p.setUser(u);
        profiles.save(p);
        log.info("User registered id={}", u.getId());
        return respond(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
        // identical message for unknown email and wrong password: no account enumeration
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Incorrect email or password");
        }
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account has been suspended");
        }
        return respond(u);
    }

    /** Bumping the token version revokes every token issued before logout. */
    @Transactional
    public void logout(UUID userId) {
        users.findById(userId).ifPresent(u -> u.setTokenVersion(u.getTokenVersion() + 1));
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return toDto(users.findById(userId).orElseThrow(() -> ApiException.notFound("User")));
    }

    private AuthResponse respond(User u) {
        return new AuthResponse(jwt.issue(u), jwt.ttlSeconds(), toDto(u));
    }

    static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), Set.copyOf(u.getRoles()));
    }
}
