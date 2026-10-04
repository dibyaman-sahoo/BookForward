package com.bookforward.dto;

import com.bookforward.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain a letter and a digit") String password,
            @NotBlank @Size(min = 2, max = 100) String displayName) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UserDto(UUID id, String email, String displayName, Set<Role> roles) {}
    public record AuthResponse(String token, long expiresInSeconds, UserDto user) {}
}
