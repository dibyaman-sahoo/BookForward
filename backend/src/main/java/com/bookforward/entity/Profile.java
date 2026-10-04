package com.bookforward.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 500)
    private String bio;

    @Column(length = 150)
    private String institution;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING) @Column(length = 20)
    private AcademicLevel academicLevel;

    @Column(length = 50)
    private String board;

    @Column(length = 100)
    private String targetExam;
}
