package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.Complaint;
import com.ram.civicdesk.entity.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    /** One complaint with everything the response needs, in a single query. */
    @EntityGraph(attributePaths = {"category", "category.department", "ward", "ward.zone", "citizen", "assignedOfficer"})
    Optional<Complaint> findDetailedById(Long id);

    /**
     * The main list query. Every filter is optional (null = no filter).
     * citizenId also matches complaints the citizen supported as a duplicate report.
     */
    @EntityGraph(attributePaths = {"category", "category.department", "ward", "ward.zone", "citizen", "assignedOfficer"})
    @Query("""
            SELECT c FROM Complaint c
            WHERE c.status IN :statuses
              AND (:citizenId IS NULL
                   OR c.citizen.id = :citizenId
                   OR EXISTS (SELECT h.id FROM ComplaintHistory h
                              WHERE h.complaint = c
                                AND h.actor.id = :citizenId
                                AND h.action = com.ram.civicdesk.entity.HistoryAction.DUPLICATE_REPORTED))
              AND (:scopeWardId IS NULL OR c.ward.id = :scopeWardId)
              AND (:scopeZoneId IS NULL OR c.ward.zone.id = :scopeZoneId)
              AND (:wardId IS NULL OR c.ward.id = :wardId)
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:minLevel IS NULL OR c.escalationLevel >= :minLevel)
              AND (:dueBefore IS NULL OR c.dueAt < :dueBefore)
            """)
    Page<Complaint> search(@Param("statuses") Collection<ComplaintStatus> statuses,
                           @Param("citizenId") Long citizenId,
                           @Param("scopeWardId") Long scopeWardId,
                           @Param("scopeZoneId") Integer scopeZoneId,
                           @Param("wardId") Long wardId,
                           @Param("categoryId") Integer categoryId,
                           @Param("minLevel") Integer minLevel,
                           @Param("dueBefore") LocalDateTime dueBefore,
                           Pageable pageable);

    /** Recent open complaints of the same category in the same ward, for duplicate detection. */
    @EntityGraph(attributePaths = {"citizen"})
    @Query("""
            SELECT c FROM Complaint c
            WHERE c.category.id = :categoryId
              AND c.ward.id = :wardId
              AND c.status IN :statuses
              AND c.createdAt > :since
              AND c.latitude IS NOT NULL
              AND c.longitude IS NOT NULL
            """)
    List<Complaint> findDuplicateCandidates(@Param("categoryId") Integer categoryId,
                                            @Param("wardId") Long wardId,
                                            @Param("statuses") Collection<ComplaintStatus> statuses,
                                            @Param("since") LocalDateTime since);

    /** Open complaints past their deadline that can still be escalated. */
    @EntityGraph(attributePaths = {"ward", "ward.zone"})
    @Query("""
            SELECT c FROM Complaint c
            WHERE c.status IN :statuses
              AND c.dueAt < :now
              AND c.escalationLevel < :maxLevel
            """)
    List<Complaint> findOverdueForEscalation(@Param("statuses") Collection<ComplaintStatus> statuses,
                                             @Param("now") LocalDateTime now,
                                             @Param("maxLevel") Integer maxLevel);

    /** Unassigned complaints in a ward, picked up when a new ward officer is created. */
    List<Complaint> findByWardIdAndAssignedOfficerIsNullAndStatusIn(Long wardId, Collection<ComplaintStatus> statuses);

    // ------------------------------------------------------------------
    // Analytics (native MySQL). wardId / zoneId = 0 means "no filter".
    // Columns are documented next to each query.
    // ------------------------------------------------------------------

    /** total, open, overdue, resolvedOrClosed, escalatedOpen, avgResolutionMinutes, avgRating, resolvedOnTime, resolvedWithTimestamp */
    @Query(value = """
            SELECT COUNT(c.id),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') AND c.due_at < :now THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN c.status IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') AND c.escalation_level > 0 THEN 1 ELSE 0 END), 0),
                   AVG(CASE WHEN c.resolved_at IS NOT NULL THEN TIMESTAMPDIFF(MINUTE, c.created_at, c.resolved_at) END),
                   AVG(c.rating),
                   COALESCE(SUM(CASE WHEN c.resolved_at IS NOT NULL AND c.resolved_at <= c.due_at THEN 1 ELSE 0 END), 0),
                   COUNT(c.resolved_at)
            FROM complaints c
            JOIN wards w ON w.id = c.ward_id
            WHERE (:wardId = 0 OR c.ward_id = :wardId)
              AND (:zoneId = 0 OR w.zone_id = :zoneId)
            """, nativeQuery = true)
    List<Object[]> summary(@Param("wardId") long wardId, @Param("zoneId") int zoneId, @Param("now") LocalDateTime now);

    /** status, count */
    @Query(value = """
            SELECT c.status, COUNT(c.id)
            FROM complaints c
            JOIN wards w ON w.id = c.ward_id
            WHERE (:wardId = 0 OR c.ward_id = :wardId)
              AND (:zoneId = 0 OR w.zone_id = :zoneId)
            GROUP BY c.status
            """, nativeQuery = true)
    List<Object[]> countByStatus(@Param("wardId") long wardId, @Param("zoneId") int zoneId);

    /** department, total, open, overdue, avgResolutionMinutes */
    @Query(value = """
            SELECT d.name,
                   COUNT(c.id),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') AND c.due_at < :now THEN 1 ELSE 0 END), 0),
                   AVG(CASE WHEN c.resolved_at IS NOT NULL THEN TIMESTAMPDIFF(MINUTE, c.created_at, c.resolved_at) END)
            FROM complaints c
            JOIN categories cat ON cat.id = c.category_id
            JOIN departments d ON d.id = cat.department_id
            JOIN wards w ON w.id = c.ward_id
            WHERE (:wardId = 0 OR c.ward_id = :wardId)
              AND (:zoneId = 0 OR w.zone_id = :zoneId)
            GROUP BY d.id, d.name
            ORDER BY COUNT(c.id) DESC, d.name
            """, nativeQuery = true)
    List<Object[]> statsByDepartment(@Param("wardId") long wardId, @Param("zoneId") int zoneId, @Param("now") LocalDateTime now);

    /** category, total, open, overdue, avgResolutionMinutes */
    @Query(value = """
            SELECT cat.name,
                   COUNT(c.id),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') AND c.due_at < :now THEN 1 ELSE 0 END), 0),
                   AVG(CASE WHEN c.resolved_at IS NOT NULL THEN TIMESTAMPDIFF(MINUTE, c.created_at, c.resolved_at) END)
            FROM complaints c
            JOIN categories cat ON cat.id = c.category_id
            JOIN wards w ON w.id = c.ward_id
            WHERE (:wardId = 0 OR c.ward_id = :wardId)
              AND (:zoneId = 0 OR w.zone_id = :zoneId)
            GROUP BY cat.id, cat.name
            ORDER BY COUNT(c.id) DESC, cat.name
            """, nativeQuery = true)
    List<Object[]> statsByCategory(@Param("wardId") long wardId, @Param("zoneId") int zoneId, @Param("now") LocalDateTime now);

    /** wardLabel, total, open, overdue, avgResolutionMinutes (top 10 wards by open complaints) */
    @Query(value = """
            SELECT CONCAT('Ward ', w.ward_number, ' (', z.name, ')'),
                   COUNT(c.id),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN c.status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','REOPENED') AND c.due_at < :now THEN 1 ELSE 0 END), 0),
                   AVG(CASE WHEN c.resolved_at IS NOT NULL THEN TIMESTAMPDIFF(MINUTE, c.created_at, c.resolved_at) END)
            FROM complaints c
            JOIN wards w ON w.id = c.ward_id
            JOIN zones z ON z.id = w.zone_id
            WHERE (:wardId = 0 OR c.ward_id = :wardId)
              AND (:zoneId = 0 OR w.zone_id = :zoneId)
            GROUP BY w.id, w.ward_number, z.name
            ORDER BY 3 DESC, 4 DESC, w.ward_number
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topWards(@Param("wardId") long wardId, @Param("zoneId") int zoneId, @Param("now") LocalDateTime now);

    /** day, count (complaints filed per day since :since) */
    @Query(value = """
            SELECT DATE(c.created_at), COUNT(c.id)
            FROM complaints c
            JOIN wards w ON w.id = c.ward_id
            WHERE (:wardId = 0 OR c.ward_id = :wardId)
              AND (:zoneId = 0 OR w.zone_id = :zoneId)
              AND c.created_at >= :since
            GROUP BY DATE(c.created_at)
            ORDER BY DATE(c.created_at)
            """, nativeQuery = true)
    List<Object[]> dailyCounts(@Param("wardId") long wardId, @Param("zoneId") int zoneId, @Param("since") LocalDateTime since);
}
