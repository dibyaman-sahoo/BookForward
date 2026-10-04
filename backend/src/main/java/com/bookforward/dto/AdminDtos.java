package com.bookforward.dto;

import com.bookforward.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class AdminDtos {
    private AdminDtos() {}

    public record ReportDto(UUID id, ReportTargetType targetType, UUID targetId, String targetLabel, String reason,
            String details, String reporter, ReportStatus status, String resolution, Instant createdAt) {}
    public record ResolveReportRequest(@NotNull ModerationActionType action, @NotBlank @Size(max = 500) String reason) {}
    public record ModerateListingRequest(@NotNull ModerationActionType action, @Size(max = 500) String reason) {}
    public record AdminUserDto(UUID id, String email, String displayName, Set<Role> roles, UserStatus status, Instant createdAt) {}
    public record RoleUpdateRequest(@NotEmpty Set<Role> roles) {}
    public record UserStatusRequest(@NotNull UserStatus status) {}
    public record AuditDto(UUID id, String actor, String action, String resourceType, String resourceId, String metadata, Instant createdAt) {}
}
