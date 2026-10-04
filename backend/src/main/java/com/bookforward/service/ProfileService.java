package com.bookforward.service;

import com.bookforward.dto.EngagementDtos.*;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository users;
    private final ProfileRepository profiles;

    @Transactional
    public ProfileDto get(UUID userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        return toDto(u, load(u));
    }

    @Transactional
    public ProfileDto update(UUID userId, ProfileUpdate r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Profile p = load(u);
        u.setDisplayName(r.displayName().trim());
        p.setBio(r.bio());
        p.setInstitution(r.institution());
        p.setCity(r.city());
        p.setAcademicLevel(r.academicLevel());
        p.setBoard(r.board());
        p.setTargetExam(r.targetExam());
        return toDto(u, p);
    }

    private Profile load(User u) {
        return profiles.findByUserId(u.getId()).orElseGet(() -> {
            Profile p = new Profile();
            p.setUser(u);
            return profiles.save(p);
        });
    }

    private ProfileDto toDto(User u, Profile p) {
        return new ProfileDto(u.getEmail(), u.getDisplayName(), p.getBio(), p.getInstitution(), p.getCity(),
                p.getAcademicLevel(), p.getBoard(), p.getTargetExam());
    }
}
