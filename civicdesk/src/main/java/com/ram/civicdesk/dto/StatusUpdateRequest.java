package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.ComplaintStatus;
import jakarta.validation.constraints.*;

public record StatusUpdateRequest(

        @NotNull(message = "Status is required")
        ComplaintStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note,

        /** Only when a citizen closes a complaint. */
        @Min(value = 1, message = "Rating must be 1 to 5")
        @Max(value = 5, message = "Rating must be 1 to 5")
        Integer rating,

        @Size(max = 500, message = "Feedback must be at most 500 characters")
        String feedback
) {}
