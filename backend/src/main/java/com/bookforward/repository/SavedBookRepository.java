package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface SavedBookRepository extends JpaRepository<SavedBook, UUID> {
    Optional<SavedBook> findByUserIdAndListingId(UUID userId, UUID listingId);
    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);
    Page<SavedBook> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("select s.listing.id from SavedBook s where s.user.id = :uid")
    List<UUID> findListingIdsByUser(@Param("uid") UUID userId);
}
