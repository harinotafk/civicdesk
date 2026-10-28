package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.PolicyStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PolicyResponse(
        Long id,
        String title,
        String summary,
        String body,
        Integer categoryId,
        String categoryName,
        String departmentName,
        Integer slaHours,
        PolicyStatus status,
        LocalDate effectiveFrom,
        Integer version,
        LocalDateTime updatedAt,
        String updatedBy
) {}
