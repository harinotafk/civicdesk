package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.AnalyticsResponse;
import com.ram.civicdesk.dto.AnalyticsResponse.GroupStat;
import com.ram.civicdesk.dto.AnalyticsResponse.LabelCount;
import com.ram.civicdesk.dto.AnalyticsResponse.Summary;
import com.ram.civicdesk.entity.ComplaintStatus;
import com.ram.civicdesk.entity.User;
import com.ram.civicdesk.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Dashboard numbers, scoped to what the caller oversees. */
@Service
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;
    private final AccessService accessService;

    public AnalyticsService(ComplaintRepository complaintRepository, AccessService accessService) {
        this.complaintRepository = complaintRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(Long userId) {
        User actor = accessService.loadActor(userId);

        // 0 = no filter; -1 = officer without an area (matches nothing)
        long wardId = 0;
        int zoneId = 0;
        String scope = "All of Chennai";
        switch (actor.getRole()) {
            case WARD_OFFICER -> {
                wardId = actor.getWard() != null ? actor.getWard().getId() : -1;
                scope = actor.getWard() != null
                        ? "Ward " + actor.getWard().getWardNumber() + " (" + actor.getWard().getZone().getName() + ")"
                        : "No ward assigned";
            }
            case ZONAL_OFFICER -> {
                zoneId = actor.getZone() != null ? actor.getZone().getId() : -1;
                scope = actor.getZone() != null
                        ? "Zone " + actor.getZone().getId() + " - " + actor.getZone().getName()
                        : "No zone assigned";
            }
            default -> { /* ADMIN sees everything; citizens are blocked in SecurityConfig */ }
        }

        LocalDateTime now = LocalDateTime.now();

        Object[] s = complaintRepository.summary(wardId, zoneId, now).get(0);
        long resolvedWithTimestamp = num(s[8]);
        Summary summary = new Summary(
                num(s[0]), num(s[1]), num(s[2]), num(s[3]), num(s[4]),
                minutesToHours(s[5]),
                round1(dbl(s[6])),
                resolvedWithTimestamp == 0 ? null : round1(num(s[7]) * 100.0 / resolvedWithTimestamp)
        );

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (ComplaintStatus st : ComplaintStatus.values()) statusCounts.put(st.name(), 0L);
        for (Object[] row : complaintRepository.countByStatus(wardId, zoneId)) {
            statusCounts.put(String.valueOf(row[0]), num(row[1]));
        }
        List<LabelCount> byStatus = statusCounts.entrySet().stream()
                .map(e -> new LabelCount(e.getKey(), e.getValue()))
                .toList();

        List<GroupStat> byDepartment = groupStats(complaintRepository.statsByDepartment(wardId, zoneId, now));
        List<GroupStat> byCategory = groupStats(complaintRepository.statsByCategory(wardId, zoneId, now));
        List<GroupStat> topWards = groupStats(complaintRepository.topWards(wardId, zoneId, now));

        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(13);
        Map<String, Long> perDay = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) perDay.put(firstDay.plusDays(i).toString(), 0L);
        for (Object[] row : complaintRepository.dailyCounts(wardId, zoneId, firstDay.atStartOfDay())) {
            String day = String.valueOf(row[0]);
            if (perDay.containsKey(day)) perDay.put(day, num(row[1]));
        }
        List<LabelCount> last14Days = perDay.entrySet().stream()
                .map(e -> new LabelCount(e.getKey(), e.getValue()))
                .toList();

        return new AnalyticsResponse(scope, summary, byStatus, byDepartment, byCategory, topWards, last14Days);
    }

    private static List<GroupStat> groupStats(List<Object[]> rows) {
        List<GroupStat> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new GroupStat(String.valueOf(r[0]), num(r[1]), num(r[2]), num(r[3]), minutesToHours(r[4])));
        }
        return out;
    }

    private static long num(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static Double dbl(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }

    private static Double minutesToHours(Object minutes) {
        Double m = dbl(minutes);
        return m == null ? null : round1(m / 60.0);
    }

    private static Double round1(Double v) {
        return v == null ? null : Math.round(v * 10.0) / 10.0;
    }
}
