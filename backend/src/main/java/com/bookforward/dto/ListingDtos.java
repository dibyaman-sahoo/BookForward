package com.bookforward.dto;

import com.bookforward.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ListingDtos {
    private ListingDtos() {}

    public record ListingRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 150) String author,
            @Size(max = 150) String publisher,
            @Size(max = 20) String isbn,
            @Size(max = 4000) String description,
            UUID categoryId,
            @Size(max = 80) String categoryOther,
            @NotNull AcademicLevel academicLevel,
            @Size(max = 50) String board,
            boolean ncertApplicable,
            @NotNull BookCondition bookCondition,
            @NotNull @DecimalMin("0.00") @DecimalMax("99999.99") @Digits(integer = 5, fraction = 2) BigDecimal price,
            @Size(max = 200) String addressLine, @Size(max = 150) String area, @Size(max = 100) String city,
            @Size(max = 100) String state, @Size(max = 20) String postalCode, Double latitude, Double longitude) {}
    public record ListingPatch(
            @Size(max = 4000) String description,
            BookCondition bookCondition,
            @DecimalMin("0.00") @DecimalMax("99999.99") @Digits(integer = 5, fraction = 2) BigDecimal price) {}
    public record ImageDto(UUID id, ImageType type, String url) {}
    public record ListingSummaryDto(UUID id, String title, String author, BigDecimal price, BookCondition bookCondition,
            AcademicLevel academicLevel, String board, boolean ncertApplicable, String category, String categorySlug,
            ListingStatus status, String coverUrl, UUID sellerId, String sellerName, Instant createdAt,
            String city, String state) {}
    public record ListingDetailDto(ListingSummaryDto summary, String publisher, String isbn, String description,
            UUID categoryId, List<ImageDto> images, boolean saved, Double sellerRating, long sellerReviewCount,
            String moderationReason, boolean ownedByViewer, String addressLine, String area, String postalCode,
            Double latitude, Double longitude) {}
    public record CategoryDto(UUID id, String name, String slug) {}
    public record SearchCriteria(String query, String category, AcademicLevel level, String board, Boolean ncert,
            BigDecimal minPrice, BigDecimal maxPrice, BookCondition condition, String availability, String sort,
            String city, Double nearLat, Double nearLon) {}
}
