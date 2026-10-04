package com.bookforward.service;

import com.bookforward.dto.EngagementDtos.*;
import com.bookforward.dto.PageResponse;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.notification.NotificationService;
import com.bookforward.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final ListingRepository listings;
    private final NotificationService notifications;

    /** Only the buyer of a COMPLETED order may review (the seller), once per order. */
    @Transactional
    public ReviewDto create(UUID userId, ReviewRequest r) {
        PurchaseOrder o = orders.findById(r.orderId()).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getBuyer().getId().equals(userId)) throw ApiException.notFound("Order");
        if (o.getStatus() != OrderStatus.COMPLETED) {
            throw ApiException.conflict("ORDER_NOT_COMPLETED", "You can review a seller after the order is completed");
        }
        if (reviews.existsByOrderIdAndReviewerId(o.getId(), userId)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "You have already reviewed this order");
        }
        Review rv = new Review();
        rv.setOrder(o);
        rv.setReviewer(o.getBuyer());
        rv.setReviewee(o.getSeller());
        rv.setListing(o.getListing());
        rv.setRating(r.rating());
        rv.setComment(r.comment() == null || r.comment().isBlank() ? null : r.comment().trim());
        rv.setStatus(ReviewStatus.VISIBLE);
        try {
            rv = reviews.saveAndFlush(rv);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("ALREADY_REVIEWED", "You have already reviewed this order");
        }
        notifications.notify(o.getSeller(), NotificationType.REVIEW_RECEIVED, "New review",
                o.getBuyer().getDisplayName() + " rated you " + r.rating() + "/5", "#/listing/" + o.getListing().getId());
        return toDto(rv);
    }

    @Transactional(readOnly = true)
    public ReviewListDto forListing(UUID listingId, int page, int size) {
        UUID sellerId = listings.findById(listingId).orElseThrow(() -> ApiException.notFound("Listing")).getSeller().getId();
        Page<Review> p = reviews.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReviewStatus.VISIBLE,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new ReviewListDto(reviews.averageForSeller(sellerId, ReviewStatus.VISIBLE),
                reviews.countForSeller(sellerId, ReviewStatus.VISIBLE), PageResponse.of(p, p.getContent().stream().map(this::toDto).toList()));
    }

    private ReviewDto toDto(Review r) {
        return new ReviewDto(r.getId(), r.getRating(), r.getComment(), r.getReviewer().getDisplayName(), r.getCreatedAt());
    }
}
