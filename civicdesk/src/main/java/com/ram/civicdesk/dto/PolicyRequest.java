package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.PolicyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PolicyRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Summary is required")
        @Size(max = 500, message = "Summary must be at most 500 characters")
        String summary,

        @NotBlank(message = "Policy text is required")
        @Size(max = 8000, message = "Policy text must be at most 8000 characters")
        String body,

        /** Optional: leave empty for a general policy. */
        Integer categoryId,

        LocalDate effectiveFrom,

        @NotNull(message = "Status is required")
        PolicyStatus status
) {}
