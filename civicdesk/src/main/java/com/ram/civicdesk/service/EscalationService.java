package com.ram.civicdesk.service;

import com.ram.civicdesk.entity.Complaint;
import com.ram.civicdesk.entity.ComplaintHistory;
import com.ram.civicdesk.entity.ComplaintStatus;
import com.ram.civicdesk.entity.HistoryAction;
import com.ram.civicdesk.repository.ComplaintHistoryRepository;
import com.ram.civicdesk.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The SLA engine. Any open complaint past its deadline moves one level up the chain
 * (Ward Officer -> Zonal Officer -> Commissioner) and gets a fresh, shorter window.
 */
@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);
    public static final int MAX_LEVEL = 2;

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final long extensionHours;

    public EscalationService(ComplaintRepository complaintRepository,
                             ComplaintHistoryRepository historyRepository,
                             @Value("${app.sla.escalation-extension-hours:24}") long extensionHours) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.extensionHours = extensionHours;
    }

    /** @return how many complaints were escalated */
    @Transactional
    public int escalateOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> overdue = complaintRepository.findOverdueForEscalation(ComplaintStatus.OPEN, now, MAX_LEVEL);

        for (Complaint c : overdue) {
            int next = c.getEscalationLevel() + 1;
            String note = "Missed the deadline (" + c.getDueAt().withNano(0) + "). Escalated to "
                    + Mappers.escalationLabel(next) + ".";
            c.setEscalationLevel(next);
            c.setDueAt(now.plusHours(extensionHours));
            historyRepository.save(new ComplaintHistory(c, HistoryAction.ESCALATED, c.getStatus(), c.getStatus(),
                    note, null, "System", now));
        }

        if (!overdue.isEmpty()) {
            log.info("SLA engine escalated {} complaint(s)", overdue.size());
        }
        return overdue.size();
    }
}
