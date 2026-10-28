package com.ram.civicdesk.controller;

import com.ram.civicdesk.dto.PolicyRequest;
import com.ram.civicdesk.dto.PolicyResponse;
import com.ram.civicdesk.dto.PolicyStatusRequest;
import com.ram.civicdesk.security.CurrentUser;
import com.ram.civicdesk.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Admin-only writes (enforced in SecurityConfig for /api/admin/**). */
@RestController
@RequestMapping("/api/admin/policies")
public class AdminPolicyController {

    private final PolicyService policyService;

    public AdminPolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{id}")
    public PolicyResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody PolicyRequest request) {
        return policyService.update(CurrentUser.id(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    public PolicyResponse setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @Valid @RequestBody PolicyStatusRequest request) {
        return policyService.setStatus(CurrentUser.id(jwt), id, request.status());
    }
}
