package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "moderation_actions")
@Getter
@Setter
@NoArgsConstructor
public class ModerationAction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ModerationActionType action;

    @Column(length = 500)
    private String reason;
}
