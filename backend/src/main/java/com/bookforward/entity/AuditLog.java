package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "actor_id", nullable = true)
    private User actor;

    @Column(length = 60, nullable = false)
    private String action;

    @Column(length = 40, nullable = false)
    private String resourceType;

    @Column(length = 64)
    private String resourceId;

    @Column(length = 2000)
    private String metadata;
}
