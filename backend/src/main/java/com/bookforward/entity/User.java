package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 100, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private int tokenVersion;

    private Instant lastSeenAt;

    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id")) @Column(name = "role", length = 20) @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
}
