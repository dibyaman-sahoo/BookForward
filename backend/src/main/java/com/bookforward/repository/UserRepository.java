package com.bookforward.repository;

import com.bookforward.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(String email, String name, Pageable pageable);

    @Transactional
    @Modifying
    @Query("update User u set u.lastSeenAt = :at where u.id = :id")
    void touchLastSeen(@Param("id") UUID id, @Param("at") Instant at);
}
