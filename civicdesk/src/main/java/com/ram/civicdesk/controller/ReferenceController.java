package com.ram.civicdesk.controller;

import com.ram.civicdesk.dto.CategoryResponse;
import com.ram.civicdesk.dto.WardResponse;
import com.ram.civicdesk.dto.ZoneResponse;
import com.ram.civicdesk.service.ReferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public lookup data: zones, wards and complaint categories. */
@RestController
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/api/zones")
    public List<ZoneResponse> zones() {
        return referenceService.zones();
    }

    @GetMapping("/api/zones/{id}/wards")
    public List<WardResponse> wards(@PathVariable Integer id) {
        return referenceService.wardsInZone(id);
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return referenceService.categories();
    }
}
