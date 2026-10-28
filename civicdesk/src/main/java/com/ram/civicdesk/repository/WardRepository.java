package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.Ward;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, Long> {

    @EntityGraph(attributePaths = {"zone"})
    List<Ward> findByZoneIdOrderByWardNumber(Integer zoneId);

    @EntityGraph(attributePaths = {"zone"})
    Optional<Ward> findWithZoneById(Long id);
}
