package com.bookforward.service;

import com.bookforward.dto.ListingDtos.*;
import com.bookforward.dto.PageResponse;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.mapper.ListingMapper;
import com.bookforward.notification.NotificationService;
import com.bookforward.repository.*;
import com.bookforward.security.UserPrincipal;
import com.bookforward.storage.StorageService;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ListingService {
    static final Set<ImageType> REQUIRED_IMAGES = EnumSet.of(ImageType.FRONT_COVER, ImageType.DETAILS_PAGE, ImageType.INDEX_PAGE);
    private static final int MAX_EXTRA_IMAGES = 5;

    private final ListingRepository listings;
    private final ListingImageRepository images;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final SavedBookRepository saved;
    private final PurchaseRequestRepository requests;
    private final ReviewRepository reviews;
    private final StorageService storage;
    private final ListingMapper mapper;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categories.findAllByOrderByNameAsc().stream().map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug())).toList();
    }

    @Transactional
    public ListingDetailDto create(UUID sellerId, ListingRequest r) {
        Listing l = new Listing();
        l.setSeller(users.getReferenceById(sellerId));
        apply(l, r);
        l.setStatus(ListingStatus.DRAFT);
        return detail(listings.save(l), sellerId, false);
    }

    @Transactional
    public ListingDetailDto update(UUID id, UUID userId, ListingRequest r) {
        Listing l = owned(id, userId);
        requireEditable(l);
        apply(l, r);
        return detail(l, userId, false);
    }

    @Transactional
    public ListingDetailDto patch(UUID id, UUID userId, ListingPatch p) {
        Listing l = owned(id, userId);
        requireEditable(l);
        if (p.description() != null) l.setDescription(p.description());
        if (p.bookCondition() != null) l.setBookCondition(p.bookCondition());
        if (p.price() != null) l.setPrice(p.price());
        return detail(l, userId, false);
    }

    @Transactional(readOnly = true)
    public ListingDetailDto get(UUID id, UserPrincipal viewer) {
        Listing l = listings.findById(id).orElseThrow(() -> ApiException.notFound("Listing"));
        boolean owner = viewer != null && l.getSeller().getId().equals(viewer.getId());
        boolean staff = viewer != null && viewer.isStaff();
        boolean publicStatus = l.getStatus() == ListingStatus.ACTIVE || l.getStatus() == ListingStatus.RESERVED || l.getStatus() == ListingStatus.SOLD;
        if (!publicStatus && !owner && !staff) {
            throw ApiException.notFound("Listing");
        }
        if (l.getStatus() == ListingStatus.REMOVED && !staff) {
            throw ApiException.notFound("Listing");
        }
        boolean isSaved = viewer != null && saved.existsByUserIdAndListingId(viewer.getId(), id);
        return detail(l, viewer == null ? null : viewer.getId(), isSaved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryDto> mine(UUID userId, int page, int size) {
        Page<Listing> p = listings.findBySellerIdAndStatusNot(userId, ListingStatus.REMOVED,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Order.desc("createdAt"))));
        return PageResponse.of(p, mapper.summaries(p.getContent()));
    }

    @Transactional
    public ListingDetailDto uploadImage(UUID listingId, UUID userId, ImageType type, MultipartFile file) {
        Listing l = owned(listingId, userId);
        requireEditable(l);
        List<ListingImage> existing = images.findByListingIdOrderByDisplayOrderAsc(listingId);
        if (type == ImageType.EXTRA) {
            if (existing.stream().filter(i -> i.getImageType() == ImageType.EXTRA).count() >= MAX_EXTRA_IMAGES) {
                throw ApiException.unprocessable("TOO_MANY_IMAGES", "You can add at most " + MAX_EXTRA_IMAGES + " extra images");
            }
        }
        StorageObject stored = storage.store(users.getReferenceById(userId), file);
        if (type != ImageType.EXTRA) {
            for (ListingImage old : existing) {
                if (old.getImageType() == type) {
                    images.delete(old);
                    storage.markDeleted(old.getStorageObject());
                }
            }
        }
        ListingImage img = new ListingImage();
        img.setListing(l);
        img.setStorageObject(stored);
        img.setImageType(type);
        img.setDisplayOrder(type.ordinal());
        images.save(img);
        return detail(l, userId, false);
    }

    @Transactional
    public void deleteExtraImage(UUID listingId, UUID userId, UUID imageId) {
        Listing l = owned(listingId, userId);
        requireEditable(l);
        ListingImage img = images.findById(imageId).filter(i -> i.getListing().getId().equals(listingId))
                .orElseThrow(() -> ApiException.notFound("Image"));
        if (img.getImageType() != ImageType.EXTRA) {
            throw ApiException.unprocessable("REQUIRED_IMAGE", "Required evidence images can only be replaced, not removed");
        }
        images.delete(img);
        storage.markDeleted(img.getStorageObject());
    }

    @Transactional
    public ListingDetailDto publish(UUID id, UUID userId) {
        Listing l = owned(id, userId);
        if (l.getStatus() != ListingStatus.DRAFT && l.getStatus() != ListingStatus.UNPUBLISHED) {
            throw ApiException.conflict("INVALID_STATE", "This listing cannot be published from its current state (" + l.getStatus() + ")");
        }
        assertEvidenceComplete(id);
        l.setStatus(ListingStatus.ACTIVE);
        l.setPublishedAt(Instant.now());
        l.setModerationReason(null);
        return detail(l, userId, false);
    }

    @Transactional
    public ListingDetailDto unpublish(UUID id, UUID userId) {
        Listing l = owned(id, userId);
        if (l.getStatus() != ListingStatus.ACTIVE) {
            throw ApiException.conflict("INVALID_STATE", "Only active listings can be unpublished");
        }
        l.setStatus(ListingStatus.UNPUBLISHED);
        return detail(l, userId, false);
    }

    @Transactional
    public void remove(UUID id, UUID userId) {
        Listing l = listings.findForUpdate(id).orElseThrow(() -> ApiException.notFound("Listing"));
        if (!l.getSeller().getId().equals(userId)) throw ApiException.notFound("Listing");
        if (l.getStatus() == ListingStatus.RESERVED) {
            throw ApiException.conflict("LISTING_RESERVED", "Cancel the open order before removing this listing");
        }
        if (l.getStatus() == ListingStatus.REMOVED) return;
        for (PurchaseRequest pr : requests.findByListingIdAndStatus(id, RequestStatus.PENDING)) {
            pr.setStatus(RequestStatus.REJECTED);
            notifications.notify(pr.getBuyer(), NotificationType.REQUEST_UPDATED, "Request closed",
                    "\"" + l.getTitle() + "\" is no longer available.", "#/dashboard?tab=sent");
        }
        l.setStatus(ListingStatus.REMOVED);
    }

    void assertEvidenceComplete(UUID listingId) {
        Set<ImageType> have = EnumSet.noneOf(ImageType.class);
        images.findByListingIdOrderByDisplayOrderAsc(listingId).forEach(i -> have.add(i.getImageType()));
        if (!have.containsAll(REQUIRED_IMAGES)) {
            Set<ImageType> missing = EnumSet.copyOf(REQUIRED_IMAGES);
            missing.removeAll(have);
            throw ApiException.unprocessable("MISSING_EVIDENCE_IMAGES",
                    "Three evidence photos are required (front cover, book-details page, index page). Missing: " + missing);
        }
    }

    private void apply(Listing l, ListingRequest r) {
        l.setCategory(categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("UNKNOWN_CATEGORY", "Choose a valid category")));
        l.setTitle(r.title().trim());
        l.setAuthor(r.author().trim());
        l.setPublisher(blankToNull(r.publisher()));
        l.setIsbn(blankToNull(r.isbn()));
        l.setDescription(blankToNull(r.description()));
        l.setAcademicLevel(r.academicLevel());
        l.setBoard(blankToNull(r.board()));
        l.setNcertApplicable(r.ncertApplicable());
        l.setBookCondition(r.bookCondition());
        l.setPrice(r.price());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private Listing owned(UUID id, UUID userId) {
        Listing l = listings.findById(id).orElseThrow(() -> ApiException.notFound("Listing"));
        if (!l.getSeller().getId().equals(userId)) {
            throw ApiException.notFound("Listing"); // do not reveal other users' listings (IDOR)
        }
        return l;
    }

    private void requireEditable(Listing l) {
        if (l.getStatus() == ListingStatus.RESERVED || l.getStatus() == ListingStatus.SOLD || l.getStatus() == ListingStatus.REMOVED) {
            throw ApiException.conflict("INVALID_STATE", "A " + l.getStatus().name().toLowerCase() + " listing cannot be edited");
        }
    }

    ListingDetailDto detail(Listing l, UUID viewerId, boolean isSaved) {
        List<ListingImage> imgs = images.findByListingIdOrderByDisplayOrderAsc(l.getId());
        String cover = imgs.stream().filter(i -> i.getImageType() == ImageType.FRONT_COVER).findFirst()
                .map(i -> ListingMapper.fileUrl(i.getStorageObject())).orElse(null);
        UUID sellerId = l.getSeller().getId();
        boolean owner = sellerId.equals(viewerId);
        return new ListingDetailDto(mapper.summary(l, cover), l.getPublisher(), l.getIsbn(), l.getDescription(),
                l.getCategory().getId(), imgs.stream().map(mapper::image).toList(), isSaved,
                reviews.averageForSeller(sellerId, ReviewStatus.VISIBLE), reviews.countForSeller(sellerId, ReviewStatus.VISIBLE),
                owner ? l.getModerationReason() : null, owner);
    }
}
