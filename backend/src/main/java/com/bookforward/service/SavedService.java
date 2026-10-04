package com.bookforward.service;

import com.bookforward.dto.ListingDtos.ListingSummaryDto;
import com.bookforward.dto.PageResponse;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.mapper.ListingMapper;
import com.bookforward.repository.*;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedService {
    private final SavedBookRepository saved;
    private final ListingRepository listings;
    private final UserRepository users;
    private final ListingMapper mapper;

    /** Saved listings that were later removed or hidden are still returned (with their status) so the UI can say so. */
    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryDto> list(UUID userId, int page, int size) {
        Page<SavedBook> p = saved.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        List<Listing> ls = p.getContent().stream().map(SavedBook::getListing).toList();
        return PageResponse.of(p, mapper.summaries(ls));
    }

    @Transactional(readOnly = true)
    public List<UUID> ids(UUID userId) { return saved.findListingIdsByUser(userId); }

    @Transactional
    public void save(UUID userId, UUID listingId) {
        Listing l = listings.findById(listingId).orElseThrow(() -> ApiException.notFound("Listing"));
        if (l.getStatus() != ListingStatus.ACTIVE && l.getStatus() != ListingStatus.RESERVED) {
            throw ApiException.conflict("LISTING_UNAVAILABLE", "This listing is not available to save");
        }
        if (saved.existsByUserIdAndListingId(userId, listingId)) return; // idempotent
        SavedBook s = new SavedBook();
        s.setUser(users.getReferenceById(userId));
        s.setListing(l);
        try {
            saved.saveAndFlush(s);
        } catch (DataIntegrityViolationException e) {
            // concurrent duplicate save: unique constraint already guarantees a single row
        }
    }

    @Transactional
    public void unsave(UUID userId, UUID listingId) {
        saved.findByUserIdAndListingId(userId, listingId).ifPresent(saved::delete);
    }
}
