package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private NotificationType notificationType;

    @Column(length = 150, nullable = false)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(length = 255)
    private String link;

    @Column(nullable = false)
    private boolean seen;
}
