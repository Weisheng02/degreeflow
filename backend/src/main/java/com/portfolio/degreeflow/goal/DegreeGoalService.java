package com.portfolio.degreeflow.goal;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.portfolio.degreeflow.common.ConflictException;
import com.portfolio.degreeflow.common.NotFoundException;
import com.portfolio.degreeflow.subject.StudySubject;
import com.portfolio.degreeflow.subject.StudySubjectRepository;
import com.portfolio.degreeflow.workspace.StudentWorkspaceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DegreeGoalService {

    private static final List<GoalStatus> BLOCKING_STATUSES = List.of(GoalStatus.TODO, GoalStatus.IN_PROGRESS);
    private static final long NEW_GOAL_SENTINEL = -1L;

    private final DegreeGoalRepository repository;
    private final StudySubjectRepository subjectRepository;
    private final StudentWorkspaceRepository workspaceRepository;
    private final Clock clock;

    public DegreeGoalService(DegreeGoalRepository repository, StudySubjectRepository subjectRepository,
            StudentWorkspaceRepository workspaceRepository) {
        this.repository = repository;
        this.subjectRepository = subjectRepository;
        this.workspaceRepository = workspaceRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public List<DegreeGoal> list(String ownerEmail, boolean reviewer) {
        return reviewer
                ? repository.findByOwnerEmailAndPortfolioVisibleTrueOrderByDueAtAsc(ownerEmail)
                : repository.findByOwnerEmailOrderByDueAtAsc(ownerEmail);
    }

    @Transactional
    public DegreeGoal create(String ownerEmail, GoalCommand command) {
        validate(command);
        lockWorkspace(ownerEmail);
        StudySubject subject = findActiveSubject(command.subjectId(), ownerEmail);
        preventStudySessionOverlap(ownerEmail, command, NEW_GOAL_SENTINEL);
        Instant now = clock.instant();
        return repository.save(new DegreeGoal(subject, ownerEmail, command.title().trim(), command.goalType(),
                command.priority(), command.plannedStart(), command.dueAt(), command.notes().trim(),
                normalizeUrl(command.evidenceUrl()), command.portfolioVisible(), now));
    }

    @Transactional
    public DegreeGoal update(Long id, String ownerEmail, GoalCommand command) {
        validate(command);
        lockWorkspace(ownerEmail);
        DegreeGoal goal = findOwned(id, ownerEmail);
        StudySubject subject = findActiveSubject(command.subjectId(), ownerEmail);
        preventStudySessionOverlap(ownerEmail, command, id);
        goal.update(subject, command.title().trim(), command.goalType(), command.priority(),
                command.plannedStart(), command.dueAt(), command.notes().trim(),
                normalizeUrl(command.evidenceUrl()), command.portfolioVisible(), clock.instant());
        return goal;
    }

    @Transactional
    public DegreeGoal changeStatus(Long id, String ownerEmail, GoalStatus target) {
        DegreeGoal goal = findOwned(id, ownerEmail);
        goal.moveTo(target, clock.instant());
        return goal;
    }

    private DegreeGoal findOwned(Long id, String ownerEmail) {
        return repository.findOwnedWithSubject(id, ownerEmail)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
    }

    private StudySubject findActiveSubject(Long subjectId, String ownerEmail) {
        return subjectRepository.findByIdAndOwnerEmail(subjectId, ownerEmail)
                .filter(StudySubject::isActive)
                .orElseThrow(() -> new NotFoundException("Active subject not found"));
    }

    private void lockWorkspace(String ownerEmail) {
        workspaceRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new NotFoundException("Student workspace not found"));
    }

    private void preventStudySessionOverlap(String ownerEmail, GoalCommand command, Long excludedId) {
        if (command.goalType() != GoalType.STUDY_SESSION) {
            return;
        }
        if (repository.existsByOwnerEmailAndGoalTypeAndStatusInAndPlannedStartLessThanAndDueAtGreaterThanAndIdNot(
                ownerEmail, GoalType.STUDY_SESSION, BLOCKING_STATUSES,
                command.dueAt(), command.plannedStart(), excludedId)) {
            throw new ConflictException("This study session overlaps another active study session");
        }
    }

    private void validate(GoalCommand command) {
        if (command.goalType() == GoalType.STUDY_SESSION && command.plannedStart() == null) {
            throw new IllegalArgumentException("Study sessions require a planned start time");
        }
        if (command.plannedStart() != null && !command.dueAt().isAfter(command.plannedStart())) {
            throw new IllegalArgumentException("Due time must be after the planned start time");
        }
        if (command.portfolioVisible()
                && command.goalType() != GoalType.PROJECT_MILESTONE
                && command.goalType() != GoalType.PORTFOLIO) {
            throw new IllegalArgumentException("Only project or portfolio goals can be shared with reviewers");
        }
        validateUrl(command.evidenceUrl());
    }

    private void validateUrl(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(value.trim());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Evidence URL must use http or https");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Evidence URL must be a valid http or https URL");
        }
    }

    private String normalizeUrl(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record GoalCommand(Long subjectId, String title, GoalType goalType, GoalPriority priority,
            Instant plannedStart, Instant dueAt, String notes, String evidenceUrl, boolean portfolioVisible) {
    }
}
