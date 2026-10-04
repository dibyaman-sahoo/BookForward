package com.bookforward.dto;

import com.bookforward.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class EngagementDtos {
    private EngagementDtos() {}

    public record NotificationDto(UUID id, NotificationType type, String title, String body, String link, boolean seen, Instant createdAt) {}
    public record ReviewRequest(@NotNull UUID orderId, @Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}
    public record ReviewDto(UUID id, int rating, String comment, String reviewerName, Instant createdAt) {}
    public record ReviewListDto(Double average, long count, PageResponse<ReviewDto> reviews) {}
    public record ReportRequest(@NotNull ReportTargetType targetType, @NotNull UUID targetId,
            @NotBlank @Size(max = 50) String reason, @Size(max = 1000) String details) {}
    public record ProfileDto(String email, String displayName, String bio, String institution, String city,
            AcademicLevel academicLevel, String board, String targetExam) {}
    public record ProfileUpdate(@NotBlank @Size(min = 2, max = 100) String displayName, @Size(max = 500) String bio,
            @Size(max = 150) String institution, @Size(max = 100) String city, AcademicLevel academicLevel,
            @Size(max = 50) String board, @Size(max = 100) String targetExam) {}
    public record FeaturesDto(boolean paymentEnabled) {}
}
