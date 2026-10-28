package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.Policy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @EntityGraph(attributePaths = {"category", "category.department", "updatedBy"})
    List<Policy> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"category", "category.department", "updatedBy"})
    Optional<Policy> findWithDetailsById(Long id);
}
