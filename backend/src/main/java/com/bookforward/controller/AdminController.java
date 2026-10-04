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

import com.bookforward.admin.ModerationService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
public class AdminController {
    private final ModerationService moderation;

    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ReportRequest r) { moderation.report(p.getId(), r); }

    @GetMapping("/api/admin/reports")
    public PageResponse<ReportDto> reports(@AuthenticationPrincipal UserPrincipal p, @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return moderation.reports(p.getId(), status, page, size); }

    @PostMapping("/api/admin/reports/{id}/resolve")
    public ReportDto resolve(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ResolveReportRequest r) { return moderation.resolve(p.getId(), id, r); }

    @GetMapping("/api/admin/listings")
    public PageResponse<ListingSummaryDto> listings(@AuthenticationPrincipal UserPrincipal p, @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return moderation.listings(p.getId(), status, page, size); }

    @PostMapping("/api/admin/listings/{id}/moderate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moderate(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ModerateListingRequest r) { moderation.moderateListing(p.getId(), id, r); }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AdminUserDto> users(@AuthenticationPrincipal UserPrincipal p, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return moderation.users(p.getId(), q, page, size); }

    @PutMapping("/api/admin/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserDto roles(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody RoleUpdateRequest r) { return moderation.updateRoles(p.getId(), id, r.roles()); }

    @PatchMapping("/api/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserDto status(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody UserStatusRequest r) { return moderation.updateStatus(p.getId(), id, r.status()); }

    @GetMapping("/api/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditDto> audit(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) { return moderation.audit(p.getId(), page, size); }
}
