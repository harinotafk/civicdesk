package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.ComplaintHistory;
import com.ram.civicdesk.entity.HistoryAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    List<ComplaintHistory> findByComplaintIdOrderByCreatedAtAscIdAsc(Long complaintId);

    boolean existsByComplaintIdAndActorIdAndAction(Long complaintId, Long actorId, HistoryAction action);
}
