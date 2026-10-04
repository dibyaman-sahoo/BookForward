package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storage_objects")
@Getter
@Setter
@NoArgsConstructor
public class StorageObject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 255, nullable = false, unique = true)
    private String objectKey;

    @Column(length = 30, nullable = false)
    private String provider;

    @Column(length = 100, nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 64, nullable = false)
    private String checksum;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StorageState state;
}
