package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listing_images")
@Getter
@Setter
@NoArgsConstructor
public class ListingImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "storage_object_id", nullable = false)
    private StorageObject storageObject;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ImageType imageType;

    @Column(nullable = false)
    private int displayOrder;
}
