package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface ListingRepository extends JpaRepository<Listing, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Listing> {
    Page<Listing> findBySellerIdAndStatusNot(UUID sellerId, ListingStatus status, Pageable pageable);
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> findForUpdate(@Param("id") UUID id);
}
