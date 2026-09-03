package com.portfolio.degreeflow.goal;

import java.time.Instant;

import com.portfolio.degreeflow.subject.StudySubject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "degree_goal")
public class DegreeGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private StudySubject subject;

    @Column(name = "owner_email", nullable = false, length = 160)
    private String ownerEmail;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 32)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalPriority priority;

    @Column(name = "planned_start")
    private Instant plannedStart;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(nullable = false, length = 1000)
    private String notes;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "portfolio_visible", nullable = false)
    private boolean portfolioVisible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GoalStatus status = GoalStatus.TODO;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected DegreeGoal() {
    }

    public DegreeGoal(StudySubject subject, String ownerEmail, String title, GoalType goalType,
            GoalPriority priority, Instant plannedStart, Instant dueAt, String notes,
            String evidenceUrl, boolean portfolioVisible, Instant now) {
        this.subject = subject;
        this.ownerEmail = ownerEmail;
        this.title = title;
        this.goalType = goalType;
        this.priority = priority;
        this.plannedStart = plannedStart;
        this.dueAt = dueAt;
        this.notes = notes;
        this.evidenceUrl = evidenceUrl;
        this.portfolioVisible = portfolioVisible;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(StudySubject subject, String title, GoalType goalType, GoalPriority priority,
            Instant plannedStart, Instant dueAt, String notes, String evidenceUrl,
            boolean portfolioVisible, Instant now) {
        ensureEditable();
        this.subject = subject;
        this.title = title;
        this.goalType = goalType;
        this.priority = priority;
        this.plannedStart = plannedStart;
        this.dueAt = dueAt;
        this.notes = notes;
        this.evidenceUrl = evidenceUrl;
        this.portfolioVisible = portfolioVisible;
        this.updatedAt = now;
    }

    public void moveTo(GoalStatus target, Instant now) {
        boolean allowed = switch (status) {
            case TODO -> target == GoalStatus.IN_PROGRESS || target == GoalStatus.COMPLETED
                    || target == GoalStatus.CANCELLED;
            case IN_PROGRESS -> target == GoalStatus.COMPLETED || target == GoalStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Goal cannot move from " + status + " to " + target);
        }
        status = target;
        completedAt = target == GoalStatus.COMPLETED ? now : null;
        updatedAt = now;
    }

    private void ensureEditable() {
        if (status == GoalStatus.COMPLETED || status == GoalStatus.CANCELLED) {
            throw new IllegalStateException("Completed or cancelled goals cannot be edited");
        }
    }

    public Long getId() { return id; }
    public StudySubject getSubject() { return subject; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getTitle() { return title; }
    public GoalType getGoalType() { return goalType; }
    public GoalPriority getPriority() { return priority; }
    public Instant getPlannedStart() { return plannedStart; }
    public Instant getDueAt() { return dueAt; }
    public String getNotes() { return notes; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public boolean isPortfolioVisible() { return portfolioVisible; }
    public GoalStatus getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
