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
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profiles;
    private final ListingService listings;
    private final NotificationSupport support;

    @GetMapping("/api/profile")
    public ProfileDto get(@AuthenticationPrincipal UserPrincipal p) { return profiles.get(p.getId()); }

    @PutMapping("/api/profile")
    public ProfileDto update(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ProfileUpdate r) { return profiles.update(p.getId(), r); }

    @GetMapping("/api/categories")
    public List<CategoryDto> categories() { return listings.categories(); }

    @GetMapping("/api/config")
    public FeaturesDto config() { return support.features(); }
}
