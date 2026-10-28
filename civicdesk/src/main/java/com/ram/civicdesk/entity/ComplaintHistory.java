package com.ram.civicdesk.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_history", indexes = {
        @Index(name = "idx_history_complaint", columnList = "complaint_id, created_at")
})
public class ComplaintHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private HistoryAction action;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "from_status", length = 20)
    private ComplaintStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "to_status", length = 20)
    private ComplaintStatus toStatus;

    @Column(length = 500)
    private String note;

    /** Null when the system did it (e.g. auto-escalation). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_label", nullable = false, length = 120)
    private String actorLabel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ComplaintHistory() {}

    public ComplaintHistory(Complaint complaint, HistoryAction action, ComplaintStatus fromStatus,
                            ComplaintStatus toStatus, String note, User actor, String actorLabel,
                            LocalDateTime createdAt) {
        this.complaint = complaint;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.actor = actor;
        this.actorLabel = actorLabel;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public HistoryAction getAction() { return action; }
    public ComplaintStatus getFromStatus() { return fromStatus; }
    public ComplaintStatus getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public User getActor() { return actor; }
    public String getActorLabel() { return actorLabel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
