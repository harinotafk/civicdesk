package com.ram.civicdesk.controller;

import com.ram.civicdesk.dto.PolicyResponse;
import com.ram.civicdesk.entity.PolicyStatus;
import com.ram.civicdesk.service.PolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read access to policies for any logged-in user. Admins also see drafts and archived ones. */
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<PolicyResponse> list(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) Integer categoryId,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) PolicyStatus status) {
        return policyService.list(isAdmin(jwt), categoryId, q, status);
    }

    @GetMapping("/{id}")
    public PolicyResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return policyService.get(isAdmin(jwt), id);
    }

    private static boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
