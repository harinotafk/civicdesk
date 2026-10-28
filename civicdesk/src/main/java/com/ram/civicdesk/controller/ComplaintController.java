package com.ram.civicdesk.controller;

import com.ram.civicdesk.dto.*;
import com.ram.civicdesk.entity.ComplaintStatus;
import com.ram.civicdesk.security.CurrentUser;
import com.ram.civicdesk.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /** File a complaint (citizens only). Returns 200 instead of 201 when merged into an existing duplicate. */
    @PostMapping
    public ResponseEntity<ComplaintResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody CreateComplaintRequest request) {
        ComplaintResponse result = complaintService.create(CurrentUser.id(jwt), request);
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }

    /**
     * Complaints visible to the caller. All filters are optional:
     * ?status=SUBMITTED,ASSIGNED&wardId=170&categoryId=3&overdue=true&escalated=true&page=0&size=20&sort=dueAt,asc
     */
    @GetMapping
    public PageResponse<ComplaintResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) List<ComplaintStatus> status,
            @RequestParam(required = false) Long wardId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "false") boolean escalated,
            @PageableDefault(size = 20, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return complaintService.list(CurrentUser.id(jwt), status, wardId, categoryId, overdue, escalated, pageable);
    }

    @GetMapping("/{id}")
    public ComplaintResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return complaintService.get(CurrentUser.id(jwt), id);
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return complaintService.history(CurrentUser.id(jwt), id);
    }

    @PatchMapping("/{id}/status")
    public ComplaintResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        return complaintService.updateStatus(CurrentUser.id(jwt), id, request);
    }
}
