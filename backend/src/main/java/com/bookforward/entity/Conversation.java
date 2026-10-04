package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "listing_id", nullable = true)
    private Listing listing;

    @Column(length = 120, nullable = false, unique = true)
    private String participantKey;
}
