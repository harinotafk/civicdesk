package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.ComplaintStatus;

import java.time.LocalDateTime;

public record ComplaintResponse(
        Long id,
        ComplaintStatus status,
        Integer categoryId,
        String categoryName,
        String departmentName,
        Long wardId,
        Integer wardNumber,
        String wardName,
        Integer zoneId,
        String zoneName,
        String description,
        String landmark,
        Double latitude,
        Double longitude,
        Integer escalationLevel,
        String escalationLabel,
        Integer supportCount,
        boolean overdue,
        Long citizenId,
        String citizenName,
        String assignedOfficerName,
        LocalDateTime createdAt,
        LocalDateTime dueAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        String resolutionNote,
        Integer rating,
        String feedback,
        /** True when this request was merged into an existing complaint as a duplicate. */
        boolean duplicate
) {}
