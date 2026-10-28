package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        Long wardId,
        Integer wardNumber,
        Integer zoneId,
        String zoneName,
        boolean active,
        LocalDateTime createdAt
) {}
