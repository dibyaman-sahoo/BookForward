package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {
    boolean existsByListingIdAndBuyerIdAndStatus(UUID listingId, UUID buyerId, RequestStatus status);
    List<PurchaseRequest> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);
    List<PurchaseRequest> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);
    List<PurchaseRequest> findByListingIdAndStatus(UUID listingId, RequestStatus status);
}
