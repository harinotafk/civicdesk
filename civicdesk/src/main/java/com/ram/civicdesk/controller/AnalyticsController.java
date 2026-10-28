package com.ram.civicdesk.controller;

import com.ram.civicdesk.dto.AnalyticsResponse;
import com.ram.civicdesk.security.CurrentUser;
import com.ram.civicdesk.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** Officers and admins: numbers for the area they oversee. */
    @GetMapping("/api/analytics")
    public AnalyticsResponse analytics(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.analytics(CurrentUser.id(jwt));
    }
}
