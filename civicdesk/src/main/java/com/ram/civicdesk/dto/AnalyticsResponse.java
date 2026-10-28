package com.ram.civicdesk.dto;

import java.util.List;

public record AnalyticsResponse(
        String scope,
        Summary summary,
        List<LabelCount> byStatus,
        List<GroupStat> byDepartment,
        List<GroupStat> byCategory,
        List<GroupStat> topWards,
        List<LabelCount> last14Days
) {

    public record Summary(
            long total,
            long open,
            long overdue,
            long resolvedOrClosed,
            long escalatedOpen,
            Double avgResolutionHours,
            Double avgRating,
            Double resolvedOnTimePercent
    ) {}

    public record LabelCount(String label, long count) {}

    public record GroupStat(String label, long total, long open, long overdue, Double avgResolutionHours) {}
}
