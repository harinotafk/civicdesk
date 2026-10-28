package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.Role;
import jakarta.validation.constraints.*;

public record CreateOfficerRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        /** Required for WARD_OFFICER. */
        Long wardId,

        /** Required for ZONAL_OFFICER. */
        Integer zoneId
) {}
