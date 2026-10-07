package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
public class Listing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 150)
    private String author;

    @Column(length = 150)
    private String publisher;

    @Column(length = 20)
    private String isbn;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AcademicLevel academicLevel;

    @Column(length = 50)
    private String board;

    @Column(nullable = false)
    private boolean ncertApplicable;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookCondition bookCondition;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ListingStatus status;

    @Column(length = 200)
    private String addressLine;

    @Column(length = 150)
    private String area;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @Column(length = 500)
    private String moderationReason;

    private Instant publishedAt;
}
