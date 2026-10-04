package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(length = 2000, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MessageType messageType;
}
