package com.bookforward.controller;

import com.bookforward.dto.*;
import com.bookforward.dto.AuthDtos.*;
import com.bookforward.dto.ListingDtos.*;
import com.bookforward.dto.TradeDtos.*;
import com.bookforward.dto.ChatDtos.*;
import com.bookforward.dto.EngagementDtos.*;
import com.bookforward.dto.AdminDtos.*;
import com.bookforward.entity.*;
import com.bookforward.security.UserPrincipal;
import com.bookforward.service.*;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserPrincipal p) { auth.logout(p.getId()); }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserPrincipal p) { return auth.me(p.getId()); }
}
