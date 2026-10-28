package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.PolicyStatus;
import jakarta.validation.constraints.NotNull;

public record PolicyStatusRequest(@NotNull(message = "Status is required") PolicyStatus status) {}
