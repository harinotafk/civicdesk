package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.CreateOfficerRequest;
import com.ram.civicdesk.dto.PageResponse;
import com.ram.civicdesk.dto.UserResponse;
import com.ram.civicdesk.entity.*;
import com.ram.civicdesk.exception.BadRequestException;
import com.ram.civicdesk.exception.ConflictException;
import com.ram.civicdesk.exception.ResourceNotFoundException;
import com.ram.civicdesk.repository.ComplaintHistoryRepository;
import com.ram.civicdesk.repository.ComplaintRepository;
import com.ram.civicdesk.repository.UserRepository;
import com.ram.civicdesk.repository.WardRepository;
import com.ram.civicdesk.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final WardRepository wardRepository;
    private final ZoneRepository zoneRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, WardRepository wardRepository, ZoneRepository zoneRepository,
                        ComplaintRepository complaintRepository, ComplaintHistoryRepository historyRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.wardRepository = wardRepository;
        this.zoneRepository = zoneRepository;
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<UserResponse> result = userRepository
                .findAllWithLocation(PageRequest.of(safePage, safeSize, Sort.by("id")))
                .map(Mappers::user);
        return PageResponse.from(result);
    }

    /** Creates a ward officer, zonal officer or admin. Citizens sign up themselves. */
    @Transactional
    public UserResponse createStaff(CreateOfficerRequest req) {
        if (req.role() == Role.CITIZEN) {
            throw new BadRequestException("Citizens register themselves. Choose an officer or admin role.");
        }

        String email = AuthService.normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new ConflictException("Phone is already registered");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        Ward ward = null;
        switch (req.role()) {
            case WARD_OFFICER -> {
                if (req.wardId() == null) throw new BadRequestException("A ward officer needs a ward");
                ward = wardRepository.findWithZoneById(req.wardId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ward " + req.wardId() + " not found"));
                user.setWard(ward);
            }
            case ZONAL_OFFICER -> {
                if (req.zoneId() == null) throw new BadRequestException("A zonal officer needs a zone");
                Zone zone = zoneRepository.findById(req.zoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Zone " + req.zoneId() + " not found"));
                user.setZone(zone);
            }
            default -> { /* ADMIN: no area */ }
        }

        User saved = userRepository.save(user);

        if (ward != null) {
            pickUpUnassigned(saved, ward);
        }
        return Mappers.user(saved);
    }

    /** A new ward officer takes over open complaints in their ward that had nobody assigned. */
    private void pickUpUnassigned(User officer, Ward ward) {
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> waiting = complaintRepository.findByWardIdAndAssignedOfficerIsNullAndStatusIn(
                ward.getId(), EnumSet.of(ComplaintStatus.SUBMITTED, ComplaintStatus.REOPENED, ComplaintStatus.IN_PROGRESS));
        for (Complaint c : waiting) {
            ComplaintStatus from = c.getStatus();
            c.setAssignedOfficer(officer);
            ComplaintStatus to = from == ComplaintStatus.SUBMITTED ? ComplaintStatus.ASSIGNED : from;
            c.setStatus(to);
            historyRepository.save(new ComplaintHistory(c, HistoryAction.ASSIGNED, from, to,
                    "Assigned to new ward officer " + officer.getName(), null, "System", now));
        }
    }

    @Transactional
    public UserResponse setActive(Long adminId, Long userId, boolean active) {
        if (adminId.equals(userId) && !active) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        User user = userRepository.findWithLocationById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));
        user.setActive(active);
        return Mappers.user(user);
    }
}
