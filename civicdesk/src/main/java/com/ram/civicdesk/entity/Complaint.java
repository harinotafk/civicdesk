package com.ram.civicdesk.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_status_due", columnList = "status, due_at"),
        @Index(name = "idx_ward_status", columnList = "ward_id, status")
})
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id")
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ward_id")
    private Ward ward;

    /** The ward officer responsible; null when the ward has no officer yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 200)
    private String landmark;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status;

    /** 0 = Ward Officer, 1 = Zonal Officer, 2 = Commissioner (Admin). */
    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel = 0;

    /** How many citizens reported this same issue (1 = only the original reporter). */
    @Column(name = "support_count", nullable = false)
    private Integer supportCount = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    /** Citizen's rating 1-5, given when closing. */
    private Integer rating;

    @Column(length = 500)
    private String feedback;

    public Complaint() {}

    public Long getId() { return id; }
    public User getCitizen() { return citizen; }
    public Category getCategory() { return category; }
    public Ward getWard() { return ward; }
    public User getAssignedOfficer() { return assignedOfficer; }
    public String getDescription() { return description; }
    public String getLandmark() { return landmark; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public ComplaintStatus getStatus() { return status; }
    public Integer getEscalationLevel() { return escalationLevel; }
    public Integer getSupportCount() { return supportCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public Integer getRating() { return rating; }
    public String getFeedback() { return feedback; }

    public void setCitizen(User citizen) { this.citizen = citizen; }
    public void setCategory(Category category) { this.category = category; }
    public void setWard(Ward ward) { this.ward = ward; }
    public void setAssignedOfficer(User assignedOfficer) { this.assignedOfficer = assignedOfficer; }
    public void setDescription(String description) { this.description = description; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
    public void setSupportCount(Integer supportCount) { this.supportCount = supportCount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public boolean isOverdue(LocalDateTime now) {
        return status.isOpen() && dueAt.isBefore(now);
    }
}
