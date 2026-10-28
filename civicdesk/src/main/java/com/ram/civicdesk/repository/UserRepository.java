package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.Role;
import com.ram.civicdesk.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** Loads a user with their ward (and its zone) and zone, for permission checks. */
    @EntityGraph(attributePaths = {"ward", "ward.zone", "zone"})
    Optional<User> findWithLocationById(Long id);

    /** The active ward officer for a ward, used to auto-assign new complaints. */
    Optional<User> findFirstByRoleAndWardIdAndActiveTrueOrderByIdAsc(Role role, Long wardId);

    List<User> findByRoleAndActiveTrue(Role role);

    @EntityGraph(attributePaths = {"ward", "ward.zone", "zone"})
    @Query("SELECT u FROM User u")
    Page<User> findAllWithLocation(Pageable pageable);
}
