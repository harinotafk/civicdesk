package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.PolicyRequest;
import com.ram.civicdesk.dto.PolicyResponse;
import com.ram.civicdesk.entity.Category;
import com.ram.civicdesk.entity.Policy;
import com.ram.civicdesk.entity.PolicyStatus;
import com.ram.civicdesk.exception.BadRequestException;
import com.ram.civicdesk.exception.ResourceNotFoundException;
import com.ram.civicdesk.repository.CategoryRepository;
import com.ram.civicdesk.repository.PolicyRepository;
import com.ram.civicdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Civic policies. Admins create and edit them. Everyone logged in can read the PUBLISHED ones.
 * The list is small (tens, not thousands), so filtering is done in memory.
 */
@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public PolicyService(PolicyRepository policyRepository, CategoryRepository categoryRepository,
                         UserRepository userRepository) {
        this.policyRepository = policyRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    /** Non-admins only ever see PUBLISHED policies, whatever status they ask for. */
    @Transactional(readOnly = true)
    public List<PolicyResponse> list(boolean admin, Integer categoryId, String q, PolicyStatus status) {
        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        return policyRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(p -> admin ? (status == null || p.getStatus() == status) : p.getStatus() == PolicyStatus.PUBLISHED)
                .filter(p -> categoryId == null || (p.getCategory() != null && p.getCategory().getId().equals(categoryId)))
                .filter(p -> needle == null
                        || p.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                        || p.getSummary().toLowerCase(Locale.ROOT).contains(needle)
                        || p.getBody().toLowerCase(Locale.ROOT).contains(needle))
                .map(PolicyService::toResponse)
                .toList();
    }

    /** A policy the caller may not see returns 404, the same as the complaint endpoints. */
    @Transactional(readOnly = true)
    public PolicyResponse get(boolean admin, Long id) {
        Policy p = policyRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy " + id + " not found"));
        if (!admin && p.getStatus() != PolicyStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Policy " + id + " not found");
        }
        return toResponse(p);
    }

    @Transactional
    public PolicyResponse create(Long adminId, PolicyRequest req) {
        Policy p = new Policy();
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        p.setVersion(1);
        apply(p, req);
        p.setUpdatedBy(userRepository.getReferenceById(adminId));
        Policy saved = policyRepository.save(p);
        return get(true, saved.getId());
    }

    @Transactional
    public PolicyResponse update(Long adminId, Long id, PolicyRequest req) {
        Policy p = policyRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy " + id + " not found"));
        boolean wordingChanged = !p.getTitle().equals(req.title().trim())
                || !p.getSummary().equals(req.summary().trim())
                || !p.getBody().equals(req.body().trim());
        apply(p, req);
        if (wordingChanged) p.setVersion(p.getVersion() + 1);
        p.setUpdatedAt(LocalDateTime.now());
        p.setUpdatedBy(userRepository.getReferenceById(adminId));
        return toResponse(policyRepository.save(p));
    }

    /** Publish, unpublish (back to DRAFT) or archive without touching the wording. */
    @Transactional
    public PolicyResponse setStatus(Long adminId, Long id, PolicyStatus status) {
        Policy p = policyRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy " + id + " not found"));
        p.setStatus(status);
        p.setUpdatedAt(LocalDateTime.now());
        p.setUpdatedBy(userRepository.getReferenceById(adminId));
        return toResponse(policyRepository.save(p));
    }

    private void apply(Policy p, PolicyRequest req) {
        p.setTitle(req.title().trim());
        p.setSummary(req.summary().trim());
        p.setBody(req.body().trim());
        p.setStatus(req.status());
        p.setEffectiveFrom(req.effectiveFrom());
        if (req.categoryId() == null) {
            p.setCategory(null);
        } else {
            Category c = categoryRepository.findWithDepartmentById(req.categoryId())
                    .orElseThrow(() -> new BadRequestException("Category " + req.categoryId() + " does not exist"));
            p.setCategory(c);
        }
    }

    private static PolicyResponse toResponse(Policy p) {
        Category c = p.getCategory();
        return new PolicyResponse(
                p.getId(),
                p.getTitle(),
                p.getSummary(),
                p.getBody(),
                c == null ? null : c.getId(),
                c == null ? null : c.getName(),
                c == null ? null : c.getDepartment().getName(),
                c == null ? null : c.getSlaHours(),
                p.getStatus(),
                p.getEffectiveFrom(),
                p.getVersion(),
                p.getUpdatedAt(),
                p.getUpdatedBy() == null ? "CivicDesk" : p.getUpdatedBy().getName()
        );
    }
}
