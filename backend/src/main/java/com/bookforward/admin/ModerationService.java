package com.bookforward.admin;

import com.bookforward.dto.AdminDtos.*;
import com.bookforward.dto.ListingDtos.ListingSummaryDto;
import com.bookforward.dto.PageResponse;
import com.bookforward.dto.EngagementDtos.ReportRequest;
import com.bookforward.entity.*;
import com.bookforward.exception.ApiException;
import com.bookforward.mapper.ListingMapper;
import com.bookforward.notification.NotificationService;
import com.bookforward.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Moderation and administration use cases. Role checks happen here, not only in the controller or UI. */
@Service
@RequiredArgsConstructor
public class ModerationService {
    private final ReportRepository reports;
    private final ModerationActionRepository actions;
    private final ListingRepository listings;
    private final ListingImageRepository images;
    private final ReviewRepository reviewRepo;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final AuditService audit;
    private final NotificationService notifications;
    private final ListingMapper mapper;

    // ----- user-facing reporting -----
    @Transactional
    public void report(UUID reporterId, ReportRequest r) {
        label(r.targetType(), r.targetId()); // validates that the target exists
        Report rep = new Report();
        rep.setReporter(users.getReferenceById(reporterId));
        rep.setTargetType(r.targetType());
        rep.setTargetId(r.targetId());
        rep.setReason(r.reason().trim());
        rep.setDetails(r.details() == null || r.details().isBlank() ? null : r.details().trim());
        rep.setStatus(ReportStatus.OPEN);
        reports.save(rep);
    }

    // ----- moderator functions -----
    @Transactional(readOnly = true)
    public PageResponse<ReportDto> reports(UUID actorId, ReportStatus status, int page, int size) {
        requireStaff(actorId);
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Report> p = status == null ? reports.findAllByOrderByCreatedAtDesc(pg) : reports.findByStatusOrderByCreatedAtDesc(status, pg);
        return PageResponse.of(p, p.getContent().stream().map(this::toDto).toList());
    }

    @Transactional
    public ReportDto resolve(UUID modId, UUID reportId, ResolveReportRequest r) {
        User mod = requireStaff(modId);
        Report rep = reports.findById(reportId).orElseThrow(() -> ApiException.notFound("Report"));
        if (rep.getStatus() != ReportStatus.OPEN) {
            throw ApiException.conflict("ALREADY_RESOLVED", "This report has already been handled");
        }
        switch (r.action()) {
            case DISMISS_REPORT -> rep.setStatus(ReportStatus.DISMISSED);
            case HIDE -> {
                requireTarget(rep, ReportTargetType.LISTING);
                hideListing(rep.getTargetId(), r.reason());
                rep.setStatus(ReportStatus.RESOLVED);
            }
            case SUSPEND_USER -> {
                requireTarget(rep, ReportTargetType.USER);
                setUserStatus(rep.getTargetId(), UserStatus.SUSPENDED);
                rep.setStatus(ReportStatus.RESOLVED);
            }
            case HIDE_REVIEW -> {
                requireTarget(rep, ReportTargetType.REVIEW);
                Review rv = reviewRepo.findById(rep.getTargetId()).orElseThrow(() -> ApiException.notFound("Review"));
                rv.setStatus(ReviewStatus.HIDDEN);
                rep.setStatus(ReportStatus.RESOLVED);
            }
            default -> throw ApiException.badRequest("INVALID_ACTION", "That action cannot resolve a report");
        }
        rep.setResolution(r.reason());
        rep.setResolvedBy(mod);
        recordAction(mod, rep.getTargetType(), rep.getTargetId(), r.action(), r.reason());
        audit.record(modId, "REPORT_" + r.action(), "REPORT", rep.getId(), "target=" + rep.getTargetType() + ":" + rep.getTargetId());
        return toDto(rep);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryDto> listings(UUID actorId, ListingStatus status, int page, int size) {
        requireStaff(actorId);
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Order.desc("createdAt")));
        Page<Listing> p = listings.findByStatus(status == null ? ListingStatus.ACTIVE : status, pg);
        return PageResponse.of(p, mapper.summaries(p.getContent()));
    }

    @Transactional
    public void moderateListing(UUID modId, UUID listingId, ModerateListingRequest r) {
        User mod = requireStaff(modId);
        Listing l = listings.findForUpdate(listingId).orElseThrow(() -> ApiException.notFound("Listing"));
        switch (r.action()) {
            case APPROVE -> {
                if (l.getStatus() != ListingStatus.HIDDEN && l.getStatus() != ListingStatus.REJECTED) {
                    throw ApiException.conflict("INVALID_STATE", "Only hidden or rejected listings can be approved");
                }
                boolean hasCover = images.findByListingIdOrderByDisplayOrderAsc(listingId).stream()
                        .anyMatch(i -> i.getImageType() == ImageType.FRONT_COVER);
                if (!hasCover) throw ApiException.unprocessable("MISSING_EVIDENCE_IMAGES", "Listing lacks a front cover photo");
                l.setStatus(ListingStatus.ACTIVE);
                l.setModerationReason(null);
            }
            case REJECT, HIDE -> {
                if (r.reason() == null || r.reason().isBlank()) throw ApiException.badRequest("REASON_REQUIRED", "A reason is required");
                if (l.getStatus() == ListingStatus.SOLD || l.getStatus() == ListingStatus.REMOVED) {
                    throw ApiException.conflict("INVALID_STATE", "This listing can no longer be moderated");
                }
                l.setStatus(r.action() == ModerationActionType.REJECT ? ListingStatus.REJECTED : ListingStatus.HIDDEN);
                l.setModerationReason(r.reason().trim());
            }
            default -> throw ApiException.badRequest("INVALID_ACTION", "Unsupported moderation action");
        }
        recordAction(mod, ReportTargetType.LISTING, listingId, r.action(), r.reason());
        audit.record(modId, "LISTING_" + r.action(), "LISTING", listingId, r.reason());
        notifications.notify(l.getSeller(), NotificationType.LISTING_MODERATED, switch (r.action()) { case APPROVE -> "Listing approved"; case REJECT -> "Listing rejected"; default -> "Listing hidden"; },
                "\"" + l.getTitle() + "\"" + (r.reason() == null ? "" : ": " + r.reason()), "#/listing/" + listingId);
    }

    // ----- admin-only functions -----
    @Transactional(readOnly = true)
    public PageResponse<AdminUserDto> users(UUID actorId, String q, int page, int size) {
        requireAdmin(actorId);
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Order.desc("createdAt")));
        Page<User> p = (q == null || q.isBlank()) ? users.findAll(pg)
                : users.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q.trim(), q.trim(), pg);
        return PageResponse.of(p, p.getContent().stream().map(this::toDto).toList());
    }

    @Transactional
    public AdminUserDto updateRoles(UUID adminId, UUID userId, Set<Role> roles) {
        requireAdmin(adminId);
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Set<Role> next = EnumSet.of(Role.USER);
        next.addAll(roles);
        if (adminId.equals(userId) && !next.contains(Role.ADMIN)) {
            throw ApiException.conflict("SELF_DEMOTION", "You cannot remove your own admin role");
        }
        u.setRoles(next);
        u.setTokenVersion(u.getTokenVersion() + 1); // force re-login with the new authorities
        audit.record(adminId, "USER_ROLES_CHANGED", "USER", userId, "roles=" + next);
        return toDto(u);
    }

    @Transactional
    public AdminUserDto updateStatus(UUID adminId, UUID userId, UserStatus status) {
        requireAdmin(adminId);
        if (adminId.equals(userId)) throw ApiException.conflict("SELF_SUSPENSION", "You cannot change your own status");
        User u = setUserStatus(userId, status);
        audit.record(adminId, "USER_STATUS_CHANGED", "USER", userId, "status=" + status);
        return toDto(u);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditDto> audit(UUID adminId, int page, int size) {
        requireAdmin(adminId);
        Page<AuditLog> p = auditLogs.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.of(p, p.getContent().stream().map(a -> new AuditDto(a.getId(),
                a.getActor() == null ? "system" : a.getActor().getDisplayName(), a.getAction(), a.getResourceType(),
                a.getResourceId(), a.getMetadata(), a.getCreatedAt())).toList());
    }

    // ----- helpers -----
    private User requireStaff(UUID id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.forbidden("Not allowed"));
        if (!u.getRoles().contains(Role.MODERATOR) && !u.getRoles().contains(Role.ADMIN)) throw ApiException.forbidden("Moderator access required");
        return u;
    }

    private User requireAdmin(UUID id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.forbidden("Not allowed"));
        if (!u.getRoles().contains(Role.ADMIN)) throw ApiException.forbidden("Administrator access required");
        return u;
    }

    private void requireTarget(Report rep, ReportTargetType expected) {
        if (rep.getTargetType() != expected) throw ApiException.badRequest("INVALID_ACTION", "That action does not apply to a " + rep.getTargetType() + " report");
    }

    private void hideListing(UUID listingId, String reason) {
        Listing l = listings.findForUpdate(listingId).orElseThrow(() -> ApiException.notFound("Listing"));
        if (l.getStatus() != ListingStatus.SOLD && l.getStatus() != ListingStatus.REMOVED) {
            l.setStatus(ListingStatus.HIDDEN);
            l.setModerationReason(reason);
            notifications.notify(l.getSeller(), NotificationType.LISTING_MODERATED, "Listing hidden",
                    "\"" + l.getTitle() + "\": " + reason, "#/listing/" + listingId);
        }
    }

    private User setUserStatus(UUID userId, UserStatus status) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        u.setStatus(status);
        u.setTokenVersion(u.getTokenVersion() + 1);
        return u;
    }

    private void recordAction(User mod, ReportTargetType t, UUID target, ModerationActionType a, String reason) {
        ModerationAction m = new ModerationAction();
        m.setModerator(mod);
        m.setTargetType(t);
        m.setTargetId(target);
        m.setAction(a);
        m.setReason(reason);
        actions.save(m);
    }

    private String label(ReportTargetType t, UUID id) {
        return switch (t) {
            case LISTING -> listings.findById(id).map(Listing::getTitle).orElseThrow(() -> ApiException.notFound("Listing"));
            case USER -> users.findById(id).map(User::getDisplayName).orElseThrow(() -> ApiException.notFound("User"));
            case REVIEW -> reviewRepo.findById(id).map(r -> "Review (" + r.getRating() + "/5)").orElseThrow(() -> ApiException.notFound("Review"));
        };
    }

    private ReportDto toDto(Report r) {
        String label;
        try { label = label(r.getTargetType(), r.getTargetId()); } catch (ApiException e) { label = "(deleted)"; }
        return new ReportDto(r.getId(), r.getTargetType(), r.getTargetId(), label, r.getReason(), r.getDetails(),
                r.getReporter().getDisplayName(), r.getStatus(), r.getResolution(), r.getCreatedAt());
    }

    private AdminUserDto toDto(User u) {
        return new AdminUserDto(u.getId(), u.getEmail(), u.getDisplayName(), Set.copyOf(u.getRoles()), u.getStatus(), u.getCreatedAt());
    }
}
