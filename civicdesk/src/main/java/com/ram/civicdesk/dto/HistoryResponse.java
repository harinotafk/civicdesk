package com.ram.civicdesk.dto;

import com.ram.civicdesk.entity.ComplaintStatus;
import com.ram.civicdesk.entity.HistoryAction;

import java.time.LocalDateTime;

public record HistoryResponse(
        Long id,
        HistoryAction action,
        ComplaintStatus fromStatus,
        ComplaintStatus toStatus,
        String note,
        String actorLabel,
        LocalDateTime createdAt
) {}
