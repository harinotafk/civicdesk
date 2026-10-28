package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.*;
import com.ram.civicdesk.entity.*;

import java.time.LocalDateTime;

/** Entity -> DTO conversions. Callers must be inside a transaction (relations are LAZY). */
public final class Mappers {

    private Mappers() {}

    public static String escalationLabel(int level) {
        return switch (level) {
            case 0 -> "Ward Officer";
            case 1 -> "Zonal Officer";
            default -> "Commissioner";
        };
    }

    public static ComplaintResponse complaint(Complaint c, boolean duplicate) {
        Ward ward = c.getWard();
        Zone zone = ward.getZone();
        Category category = c.getCategory();
        User officer = c.getAssignedOfficer();
        return new ComplaintResponse(
                c.getId(),
                c.getStatus(),
                category.getId(),
                category.getName(),
                category.getDepartment().getName(),
                ward.getId(),
                ward.getWardNumber(),
                ward.getName(),
                zone.getId(),
                zone.getName(),
                c.getDescription(),
                c.getLandmark(),
                c.getLatitude(),
                c.getLongitude(),
                c.getEscalationLevel(),
                escalationLabel(c.getEscalationLevel()),
                c.getSupportCount(),
                c.isOverdue(LocalDateTime.now()),
                c.getCitizen().getId(),
                c.getCitizen().getName(),
                officer == null ? null : officer.getName(),
                c.getCreatedAt(),
                c.getDueAt(),
                c.getResolvedAt(),
                c.getClosedAt(),
                c.getResolutionNote(),
                c.getRating(),
                c.getFeedback(),
                duplicate
        );
    }

    public static UserResponse user(User u) {
        Ward ward = u.getWard();
        Zone zone = u.getZone() != null ? u.getZone() : (ward != null ? ward.getZone() : null);
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                ward == null ? null : ward.getId(),
                ward == null ? null : ward.getWardNumber(),
                zone == null ? null : zone.getId(),
                zone == null ? null : zone.getName(),
                u.isActive(),
                u.getCreatedAt()
        );
    }

    public static HistoryResponse history(ComplaintHistory h) {
        return new HistoryResponse(h.getId(), h.getAction(), h.getFromStatus(), h.getToStatus(),
                h.getNote(), h.getActorLabel(), h.getCreatedAt());
    }

    public static WardResponse ward(Ward w) {
        return new WardResponse(w.getId(), w.getWardNumber(), w.getName(), w.getZone().getId(), w.getZone().getName());
    }

    public static CategoryResponse category(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlaHours(),
                c.getDepartment().getId(), c.getDepartment().getName());
    }
}
