package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.ComplaintResponse;
import com.ram.civicdesk.dto.CreateComplaintRequest;
import com.ram.civicdesk.dto.HistoryResponse;
import com.ram.civicdesk.dto.PageResponse;
import com.ram.civicdesk.dto.StatusUpdateRequest;
import com.ram.civicdesk.entity.*;
import com.ram.civicdesk.exception.BadRequestException;
import com.ram.civicdesk.exception.ForbiddenException;
import com.ram.civicdesk.exception.ResourceNotFoundException;
import com.ram.civicdesk.repository.CategoryRepository;
import com.ram.civicdesk.repository.ComplaintHistoryRepository;
import com.ram.civicdesk.repository.ComplaintRepository;
import com.ram.civicdesk.repository.UserRepository;
import com.ram.civicdesk.repository.WardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ComplaintService {

    /** Reports of the same category in the same ward within this distance are treated as one issue. */
    private static final double DUPLICATE_RADIUS_METRES = 150;
    private static final int DUPLICATE_WINDOW_HOURS = 48;

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "dueAt", "createdAt", "status", "escalationLevel", "supportCount");

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final CategoryRepository categoryRepository;
    private final WardRepository wardRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintHistoryRepository historyRepository,
                            CategoryRepository categoryRepository,
                            WardRepository wardRepository,
                            UserRepository userRepository,
                            AccessService accessService) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.categoryRepository = categoryRepository;
        this.wardRepository = wardRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Transactional
    public ComplaintResponse create(Long userId, CreateComplaintRequest req) {
        User citizen = accessService.loadActor(userId);
        if (citizen.getRole() != Role.CITIZEN) {
            throw new ForbiddenException("Only citizens can file complaints");
        }

        Category category = categoryRepository.findWithDepartmentById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + req.categoryId() + " not found"));
        Ward ward = wardRepository.findWithZoneById(req.wardId())
                .orElseThrow(() -> new ResourceNotFoundException("Ward " + req.wardId() + " not found"));

        LocalDateTime now = LocalDateTime.now();

        // 1. Same issue already reported nearby? Add support instead of creating a new ticket.
        Optional<Complaint> existing = findDuplicate(category, ward, req.latitude(), req.longitude(), now);
        if (existing.isPresent()) {
            return supportExisting(existing.get(), citizen, now);
        }

        // 2. New complaint with its SLA deadline
        Complaint c = new Complaint();
        c.setCitizen(citizen);
        c.setCategory(category);
        c.setWard(ward);
        c.setDescription(req.description().trim());
        c.setLandmark(trimToNull(req.landmark()));
        c.setLatitude(req.latitude());
        c.setLongitude(req.longitude());
        c.setStatus(ComplaintStatus.SUBMITTED);
        c.setEscalationLevel(0);
        c.setSupportCount(1);
        c.setCreatedAt(now);
        c.setDueAt(now.plusHours(category.getSlaHours()));
        complaintRepository.save(c);

        addHistory(c, HistoryAction.CREATED, null, ComplaintStatus.SUBMITTED,
                "Filed under " + category.getName(), citizen, now);

        // 3. Auto-route to the ward's officer, if the ward has one
        userRepository.findFirstByRoleAndWardIdAndActiveTrueOrderByIdAsc(Role.WARD_OFFICER, ward.getId())
                .ifPresent(officer -> {
                    c.setAssignedOfficer(officer);
                    c.setStatus(ComplaintStatus.ASSIGNED);
                    addHistory(c, HistoryAction.ASSIGNED, ComplaintStatus.SUBMITTED, ComplaintStatus.ASSIGNED,
                            "Auto-assigned to " + officer.getName() + " (Ward Officer)", null, now);
                });

        return Mappers.complaint(c, false);
    }

    private Optional<Complaint> findDuplicate(Category category, Ward ward, Double lat, Double lng, LocalDateTime now) {
        if (lat == null || lng == null) {
            return Optional.empty();
        }
        return complaintRepository
                .findDuplicateCandidates(category.getId(), ward.getId(), ComplaintStatus.OPEN,
                        now.minusHours(DUPLICATE_WINDOW_HOURS))
                .stream()
                .filter(c -> distanceMetres(lat, lng, c.getLatitude(), c.getLongitude()) <= DUPLICATE_RADIUS_METRES)
                .min(Comparator.comparing(Complaint::getCreatedAt));
    }

    private ComplaintResponse supportExisting(Complaint c, User citizen, LocalDateTime now) {
        boolean alreadyCounted = c.getCitizen().getId().equals(citizen.getId())
                || historyRepository.existsByComplaintIdAndActorIdAndAction(
                        c.getId(), citizen.getId(), HistoryAction.DUPLICATE_REPORTED);
        if (!alreadyCounted) {
            c.setSupportCount(c.getSupportCount() + 1);
            addHistory(c, HistoryAction.DUPLICATE_REPORTED, c.getStatus(), c.getStatus(),
                    "Same issue reported by another citizen (" + c.getSupportCount() + " reports now)", citizen, now);
        }
        return Mappers.complaint(c, true);
    }

    /** Haversine distance between two points on Earth, in metres. */
    static double distanceMetres(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ComplaintResponse get(Long userId, Long id) {
        User actor = accessService.loadActor(userId);
        return Mappers.complaint(loadVisible(actor, id), false);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long userId, Long id) {
        User actor = accessService.loadActor(userId);
        loadVisible(actor, id);
        return historyRepository.findByComplaintIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(Mappers::history)
                .toList();
    }

    /**
     * Lists complaints the caller may see:
     * citizens their own (and ones they supported), ward officers their ward,
     * zonal officers their zone, admins everything.
     */
    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> list(Long userId,
                                                List<ComplaintStatus> statuses,
                                                Long wardId,
                                                Integer categoryId,
                                                boolean overdueOnly,
                                                boolean escalatedOnly,
                                                Pageable pageable) {
        validateSort(pageable);
        User actor = accessService.loadActor(userId);

        Long citizenId = null;
        Long scopeWardId = null;
        Integer scopeZoneId = null;
        switch (actor.getRole()) {
            case CITIZEN -> citizenId = actor.getId();
            case WARD_OFFICER -> scopeWardId = actor.getWard() != null ? actor.getWard().getId() : -1L;
            case ZONAL_OFFICER -> scopeZoneId = actor.getZone() != null ? actor.getZone().getId() : -1;
            case ADMIN -> { /* no restriction */ }
        }

        Collection<ComplaintStatus> effectiveStatuses;
        if (statuses != null && !statuses.isEmpty()) {
            effectiveStatuses = statuses;
        } else if (actor.getRole() == Role.CITIZEN && !overdueOnly) {
            effectiveStatuses = EnumSet.allOf(ComplaintStatus.class); // citizens also want to see finished ones
        } else {
            effectiveStatuses = ComplaintStatus.OPEN;                 // officers care about unfinished work
        }

        LocalDateTime dueBefore = overdueOnly ? LocalDateTime.now() : null;
        Integer minLevel = escalatedOnly ? 1 : null;

        Page<ComplaintResponse> page = complaintRepository
                .search(effectiveStatuses, citizenId, scopeWardId, scopeZoneId, wardId, categoryId,
                        minLevel, dueBefore, pageable)
                .map(c -> Mappers.complaint(c, false));
        return PageResponse.from(page);
    }

    // ------------------------------------------------------------------
    // Status changes
    // ------------------------------------------------------------------

    @Transactional
    public ComplaintResponse updateStatus(Long userId, Long id, StatusUpdateRequest req) {
        User actor = accessService.loadActor(userId);
        Complaint c = loadVisible(actor, id);

        ComplaintStatus from = c.getStatus();
        ComplaintStatus to = req.status();
        if (from == to) {
            throw new BadRequestException("Complaint is already " + to);
        }

        String note = trimToNull(req.note());
        LocalDateTime now = LocalDateTime.now();
        boolean isOwner = actor.getRole() == Role.CITIZEN && c.getCitizen().getId().equals(actor.getId());

        switch (to) {
            case SUBMITTED -> throw new BadRequestException("A complaint cannot be moved back to SUBMITTED");

            case ASSIGNED -> {
                requireOfficer(actor, "assign complaints");
                requireFrom(from, to, ComplaintStatus.SUBMITTED);
                User officer = actor.getRole() == Role.WARD_OFFICER
                        ? actor
                        : userRepository.findFirstByRoleAndWardIdAndActiveTrueOrderByIdAsc(
                                Role.WARD_OFFICER, c.getWard().getId()).orElse(null);
                if (officer == null) {
                    throw new BadRequestException("This ward has no active ward officer yet. An admin can create one under Users.");
                }
                c.setAssignedOfficer(officer);
                if (note == null) note = "Assigned to " + officer.getName();
            }

            case IN_PROGRESS -> {
                requireOfficer(actor, "start work on complaints");
                requireFrom(from, to, ComplaintStatus.SUBMITTED, ComplaintStatus.ASSIGNED, ComplaintStatus.REOPENED);
                if (c.getAssignedOfficer() == null && actor.getRole() == Role.WARD_OFFICER) {
                    c.setAssignedOfficer(actor);
                }
            }

            case RESOLVED -> {
                requireOfficer(actor, "resolve complaints");
                requireFrom(from, to, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.REOPENED);
                if (note == null) {
                    throw new BadRequestException("Please describe what was done to resolve the issue");
                }
                c.setResolvedAt(now);
                c.setResolutionNote(note);
            }

            case CLOSED -> {
                if (isOwner) {
                    requireFrom(from, to, ComplaintStatus.RESOLVED);
                    c.setRating(req.rating());
                    c.setFeedback(trimToNull(req.feedback()));
                } else if (actor.getRole() == Role.ADMIN) {
                    if (from != ComplaintStatus.RESOLVED && note == null) {
                        throw new BadRequestException("Please give a reason for closing a complaint that was not resolved");
                    }
                } else {
                    throw new ForbiddenException("Only the citizen who filed the complaint, or an admin, can close it");
                }
                c.setClosedAt(now);
            }

            case REOPENED -> {
                if (!isOwner) {
                    throw new ForbiddenException("Only the citizen who filed the complaint can reopen it");
                }
                requireFrom(from, to, ComplaintStatus.RESOLVED);
                if (note == null) {
                    throw new BadRequestException("Please say why the issue is not fixed");
                }
                c.setResolvedAt(null);
                c.setDueAt(now.plusHours(c.getCategory().getSlaHours())); // a fresh deadline
            }
        }

        c.setStatus(to);
        addHistory(c, to == ComplaintStatus.ASSIGNED ? HistoryAction.ASSIGNED : HistoryAction.STATUS_CHANGED,
                from, to, note, actor, now);

        if (to == ComplaintStatus.CLOSED && isOwner && (req.rating() != null || c.getFeedback() != null)) {
            String feedbackNote = (req.rating() != null ? "Rated " + req.rating() + "/5" : "Feedback")
                    + (c.getFeedback() != null ? ": " + c.getFeedback() : "");
            addHistory(c, HistoryAction.FEEDBACK, to, to, feedbackNote, actor, now);
        }

        return Mappers.complaint(c, false);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Returns 404 (not 403) for complaints the caller may not see, so IDs can't be probed. */
    private Complaint loadVisible(User actor, Long id) {
        Complaint c = complaintRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint " + id + " not found"));
        if (!accessService.canView(actor, c)) {
            throw new ResourceNotFoundException("Complaint " + id + " not found");
        }
        return c;
    }

    private void addHistory(Complaint c, HistoryAction action, ComplaintStatus from, ComplaintStatus to,
                            String note, User actor, LocalDateTime at) {
        String label = actor == null ? "System" : actor.getName() + " (" + roleLabel(actor.getRole()) + ")";
        historyRepository.save(new ComplaintHistory(c, action, from, to, note, actor, label, at));
    }

    static String roleLabel(Role role) {
        return switch (role) {
            case CITIZEN -> "Citizen";
            case WARD_OFFICER -> "Ward Officer";
            case ZONAL_OFFICER -> "Zonal Officer";
            case ADMIN -> "Commissioner";
        };
    }

    private static void requireOfficer(User actor, String what) {
        if (actor.getRole() == Role.CITIZEN) {
            throw new ForbiddenException("Only officers can " + what);
        }
    }

    private static void requireFrom(ComplaintStatus from, ComplaintStatus to, ComplaintStatus... allowed) {
        for (ComplaintStatus s : allowed) {
            if (s == from) return;
        }
        throw new BadRequestException("Cannot change status from " + from + " to " + to);
    }

    private static void validateSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_FIELDS);
            }
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
