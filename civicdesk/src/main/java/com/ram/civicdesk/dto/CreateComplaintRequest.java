package com.ram.civicdesk.dto;

import jakarta.validation.constraints.*;

public record CreateComplaintRequest(

        @NotNull(message = "Category is required")
        Integer categoryId,

        @NotNull(message = "Ward is required")
        Long wardId,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 1000, message = "Description must be 10 to 1000 characters")
        String description,

        @Size(max = 200, message = "Landmark must be at most 200 characters")
        String landmark,

        @DecimalMin(value = "12.8", message = "Location must be within Chennai")
        @DecimalMax(value = "13.3", message = "Location must be within Chennai")
        Double latitude,

        @DecimalMin(value = "80.0", message = "Location must be within Chennai")
        @DecimalMax(value = "80.4", message = "Location must be within Chennai")
        Double longitude
) {}
