package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 50, nullable = false)
    private String reason;

    @Column(length = 1000)
    private String details;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(length = 500)
    private String resolution;

    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "resolved_by_id", nullable = true)
    private User resolvedBy;
}
