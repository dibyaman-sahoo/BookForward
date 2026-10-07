package com.bookforward.mapper;

import com.bookforward.dto.ListingDtos.*;
import com.bookforward.entity.*;
import com.bookforward.repository.ListingImageRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Entity to DTO transformations for listings. Call inside a transaction (lazy associations). */
@Component
@RequiredArgsConstructor
public class ListingMapper {
    private final ListingImageRepository images;

    public static String fileUrl(StorageObject o) { return "/api/files/" + o.getObjectKey(); }

    public Map<UUID, String> coverUrls(Collection<UUID> listingIds) {
        if (listingIds.isEmpty()) return Map.of();
        Map<UUID, String> covers = new HashMap<>();
        for (ListingImage i : images.findByListingIdInAndImageType(listingIds, ImageType.FRONT_COVER)) {
            covers.put(i.getListing().getId(), fileUrl(i.getStorageObject()));
        }
        return covers;
    }

    public ListingSummaryDto summary(Listing l, String coverUrl) {
        return new ListingSummaryDto(l.getId(), l.getTitle(), l.getAuthor() == null ? "" : l.getAuthor(), l.getPrice(), l.getBookCondition(),
                l.getAcademicLevel(), l.getBoard(), l.isNcertApplicable(), l.getCategory().getName(),
                l.getCategory().getSlug(), l.getStatus(), coverUrl, l.getSeller().getId(),
                l.getSeller().getDisplayName(), l.getCreatedAt(), l.getCity(), l.getState());
    }

    public List<ListingSummaryDto> summaries(Collection<Listing> listings) {
        Map<UUID, String> covers = coverUrls(listings.stream().map(Listing::getId).toList());
        return listings.stream().map(l -> summary(l, covers.get(l.getId()))).toList();
    }

    public ImageDto image(ListingImage i) {
        return new ImageDto(i.getId(), i.getImageType(), fileUrl(i.getStorageObject()));
    }
}
