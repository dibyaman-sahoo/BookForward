package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saved_books", uniqueConstraints = @UniqueConstraint(name = "uq_saved_user_listing", columnNames = {"user_id", "listing_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SavedBook extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
}
