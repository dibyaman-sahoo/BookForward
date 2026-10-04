package com.bookforward;

import static org.junit.jupiter.api.Assertions.*;

import com.bookforward.config.AppProperties;
import com.bookforward.entity.User;
import com.bookforward.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static AppProperties props(String secret) {
        return new AppProperties("test", "http://x", "http://x", new AppProperties.Jwt(secret, 5), null, null, null, null);
    }

    private static User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setTokenVersion(3);
        return u;
    }

    @Test
    void issuesAndParsesToken() {
        JwtService svc = new JwtService(props("0123456789abcdef0123456789abcdef"));
        User u = user();
        var claims = svc.parse(svc.issue(u));
        assertEquals(u.getId().toString(), claims.getSubject());
        assertEquals(3, claims.get("ver", Integer.class));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService a = new JwtService(props("0123456789abcdef0123456789abcdef"));
        JwtService b = new JwtService(props("ffffffffffffffffffffffffffffffff"));
        String token = a.issue(user());
        assertThrows(JwtException.class, () -> b.parse(token));
    }

    @Test
    void refusesWeakOrMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService(props("short")));
        assertThrows(IllegalStateException.class, () -> new JwtService(props("")));
    }
}
