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

import com.bookforward.search.SearchService;
import com.bookforward.payment.PaymentService;
import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class ListingController {
    private final ListingService listings;
    private final SearchService search;
    private final ReviewService reviews;
    private final SavedService saved;

    @GetMapping({"/api/listings", "/api/search/listings"})
    public PageResponse<ListingSummaryDto> search(
            @RequestParam(required = false) String query, @RequestParam(required = false) String category,
            @RequestParam(required = false) AcademicLevel level, @RequestParam(required = false) String board,
            @RequestParam(required = false) Boolean ncert, @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) BookCondition condition,
            @RequestParam(required = false) String availability, @RequestParam(required = false) String sort,
            @RequestParam(required = false) String city, @RequestParam(required = false) Double nearLat,
            @RequestParam(required = false) Double nearLon,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return search.search(new SearchCriteria(query, category, level, board, ncert, minPrice, maxPrice, condition, availability, sort, city, nearLat, nearLon), page, size);
    }

    @PostMapping("/api/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDetailDto create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ListingRequest r) { return listings.create(p.getId(), r); }

    @GetMapping("/api/listings/{id}")
    public ListingDetailDto get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { return listings.get(id, p); }

    @PutMapping("/api/listings/{id}")
    public ListingDetailDto update(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ListingRequest r) { return listings.update(id, p.getId(), r); }

    @PatchMapping("/api/listings/{id}")
    public ListingDetailDto patch(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ListingPatch r) { return listings.patch(id, p.getId(), r); }

    @DeleteMapping("/api/listings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { listings.remove(id, p.getId()); }

    @PostMapping("/api/listings/{id}/publish")
    public ListingDetailDto publish(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { return listings.publish(id, p.getId()); }

    @PostMapping("/api/listings/{id}/unpublish")
    public ListingDetailDto unpublish(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p) { return listings.unpublish(id, p.getId()); }

    @PostMapping(path = "/api/listings/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDetailDto upload(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal p,
                                   @RequestParam ImageType type, @RequestPart("file") MultipartFile file) {
        return listings.uploadImage(id, p.getId(), type, file);
    }

    @DeleteMapping("/api/listings/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable UUID id, @PathVariable UUID imageId, @AuthenticationPrincipal UserPrincipal p) { listings.deleteExtraImage(id, p.getId(), imageId); }

    @GetMapping("/api/me/listings")
    public PageResponse<ListingSummaryDto> mine(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return listings.mine(p.getId(), page, size); }

    @GetMapping("/api/listings/{id}/reviews")
    public ReviewListDto listingReviews(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return reviews.forListing(id, page, size); }

    // ----- saved books -----
    @GetMapping("/api/saved")
    public PageResponse<ListingSummaryDto> savedList(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) { return saved.list(p.getId(), page, size); }

    @GetMapping("/api/saved/ids")
    public List<UUID> savedIds(@AuthenticationPrincipal UserPrincipal p) { return saved.ids(p.getId()); }

    @PostMapping("/api/saved/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable UUID listingId, @AuthenticationPrincipal UserPrincipal p) { saved.save(p.getId(), listingId); }

    @DeleteMapping("/api/saved/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable UUID listingId, @AuthenticationPrincipal UserPrincipal p) { saved.unsave(p.getId(), listingId); }
}
