package com.bookforward.search;

import com.bookforward.dto.ListingDtos.*;
import com.bookforward.dto.PageResponse;
import com.bookforward.entity.Listing;
import com.bookforward.mapper.ListingMapper;
import com.bookforward.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ListingRepository listings;
    private final ListingMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryDto> search(SearchCriteria c, int page, int size) {
        String sort = c.sort() == null ? "relevance" : c.sort();
        boolean hasQuery = ListingSearchSpecs.hasText(c.query());
        Sort s = switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createdAt"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createdAt"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            default -> hasQuery ? Sort.unsorted() : Sort.by(Sort.Order.desc("createdAt"));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), s);
        Page<Listing> result = listings.findAll(ListingSearchSpecs.from(c), pageable);
        return PageResponse.of(result, mapper.summaries(result.getContent()));
    }
}
