package com.bookforward.config;

import com.bookforward.entity.*;
import com.bookforward.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD if (and only if) both are configured. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {
    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var a = props.bootstrapAdmin();
        if (a == null || a.email() == null || a.email().isBlank() || a.password() == null || a.password().length() < 8) {
            return;
        }
        String email = a.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            return;
        }
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(a.name());
        u.setPasswordHash(encoder.encode(a.password()));
        u.setRoles(new java.util.HashSet<>(Set.of(Role.USER, Role.MODERATOR, Role.ADMIN)));
        users.save(u);
        log.info("Bootstrap admin account created");
    }
}
