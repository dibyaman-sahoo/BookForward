package com.bookforward.security;

import com.bookforward.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TokenAuthenticator authenticator, RateLimiter limiter,
                                    AppProperties props, ObjectMapper mapper, CorsConfigurationSource cors) throws Exception {
        var jwtFilter = new JwtAuthFilter(authenticator);
        http.csrf(c -> c.disable()) // stateless bearer-token API, no cookies are used for auth
            .cors(c -> c.configurationSource(cors))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(c -> {}))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(mapper, res, 401, "UNAUTHENTICATED",
                        "Please sign in to continue", req.getRequestURI()))
                .accessDeniedHandler((req, res, ex) -> write(mapper, res, 403, "FORBIDDEN",
                        "You do not have permission to do that", req.getRequestURI())))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/ws/**").permitAll() // STOMP frames are authenticated in StompAuthInterceptor
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/*", "/api/listings/*/reviews",
                        "/api/search/**", "/api/categories", "/api/files/**", "/api/config").permitAll()
                .requestMatchers("/api/admin/**").hasAnyRole("MODERATOR", "ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new RateLimitFilter(limiter, props, mapper), JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(props.frontendOrigin().split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        c.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }

    private static void write(ObjectMapper m, jakarta.servlet.http.HttpServletResponse res, int status, String code,
                              String msg, String path) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        m.writeValue(res.getOutputStream(), Map.of("timestamp", Instant.now().toString(), "status", status,
                "code", code, "message", msg, "path", path, "fieldErrors", List.of()));
    }
}
