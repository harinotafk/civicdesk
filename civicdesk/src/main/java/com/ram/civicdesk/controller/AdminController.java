package com.ram.civicdesk.controller;

import com.ram.civicdesk.dto.*;
import com.ram.civicdesk.security.CurrentUser;
import com.ram.civicdesk.service.AdminService;
import com.ram.civicdesk.service.DemoDataService;
import com.ram.civicdesk.service.EscalationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final EscalationService escalationService;
    private final DemoDataService demoDataService;

    public AdminController(AdminService adminService, EscalationService escalationService,
                           DemoDataService demoDataService) {
        this.adminService = adminService;
        this.escalationService = escalationService;
        this.demoDataService = demoDataService;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return adminService.listUsers(page, size);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody CreateOfficerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createStaff(request));
    }

    @PatchMapping("/users/{id}/active")
    public UserResponse setActive(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody ActiveRequest request) {
        return adminService.setActive(CurrentUser.id(jwt), id, request.active());
    }

    /** Run the SLA engine now instead of waiting for the next scheduled check. */
    @PostMapping("/sla/run")
    public CountResponse runSla() {
        int n = escalationService.escalateOverdue();
        return new CountResponse(n, n == 0 ? "No overdue complaints to escalate" : n + " complaint(s) escalated");
    }

    /** Adds random complaints for demos. */
    @PostMapping("/demo/complaints")
    public CountResponse generateDemo(@RequestParam(defaultValue = "5") int count) {
        int n = demoDataService.generate(count);
        return new CountResponse(n, n + " demo complaint(s) created");
    }
}
