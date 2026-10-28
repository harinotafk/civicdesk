package com.ram.civicdesk.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the SLA engine on a timer (interval: app.sla.check-interval-ms). */
@Component
public class SlaScheduler {

    private final EscalationService escalationService;

    public SlaScheduler(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(initialDelay = 15_000, fixedDelayString = "${app.sla.check-interval-ms:60000}")
    public void run() {
        escalationService.escalateOverdue();
    }
}
