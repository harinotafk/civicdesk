package com.ram.civicdesk.service;

import com.ram.civicdesk.entity.Complaint;
import com.ram.civicdesk.entity.HistoryAction;
import com.ram.civicdesk.entity.User;
import com.ram.civicdesk.exception.UnauthorizedException;
import com.ram.civicdesk.repository.ComplaintHistoryRepository;
import com.ram.civicdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads the logged-in user and answers "may this user see this complaint?". */
@Service
public class AccessService {

    private final UserRepository userRepository;
    private final ComplaintHistoryRepository historyRepository;

    public AccessService(UserRepository userRepository, ComplaintHistoryRepository historyRepository) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    /** The current user with ward/zone loaded. Rejects deleted or deactivated accounts. */
    @Transactional(readOnly = true)
    public User loadActor(Long userId) {
        User user = userRepository.findWithLocationById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account not found. Please log in again"));
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public boolean canView(User actor, Complaint c) {
        return switch (actor.getRole()) {
            case ADMIN -> true;
            case CITIZEN -> c.getCitizen().getId().equals(actor.getId())
                    || historyRepository.existsByComplaintIdAndActorIdAndAction(
                            c.getId(), actor.getId(), HistoryAction.DUPLICATE_REPORTED);
            case WARD_OFFICER -> actor.getWard() != null
                    && actor.getWard().getId().equals(c.getWard().getId());
            case ZONAL_OFFICER -> actor.getZone() != null
                    && actor.getZone().getId().equals(c.getWard().getZone().getId());
        };
    }
}
