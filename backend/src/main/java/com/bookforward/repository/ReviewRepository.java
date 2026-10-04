package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByOrderIdAndReviewerId(UUID orderId, UUID reviewerId);
    Page<Review> findByListingIdAndStatusOrderByCreatedAtDesc(UUID listingId, ReviewStatus status, Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.reviewee.id = :uid and r.status = :st")
    Double averageForSeller(@Param("uid") UUID sellerId, @Param("st") ReviewStatus status);

    @Query("select count(r) from Review r where r.reviewee.id = :uid and r.status = :st")
    long countForSeller(@Param("uid") UUID sellerId, @Param("st") ReviewStatus status);
}
