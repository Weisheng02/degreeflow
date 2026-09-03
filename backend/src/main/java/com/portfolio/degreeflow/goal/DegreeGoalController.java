package com.portfolio.degreeflow.goal;

import java.time.Instant;
import java.util.List;

import com.portfolio.degreeflow.subject.StudySubjectController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class DegreeGoalController {

    private final DegreeGoalService service;

    public DegreeGoalController(DegreeGoalService service) {
        this.service = service;
    }

    @GetMapping
    List<GoalResponse> list(Authentication authentication) {
        boolean reviewer = isReviewer(authentication);
        String owner = reviewer ? StudySubjectController.DEMO_OWNER : authentication.getName();
        return service.list(owner, reviewer).stream().map(GoalResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    GoalResponse create(@Valid @RequestBody GoalRequest request, Authentication authentication) {
        return GoalResponse.from(service.create(authentication.getName(), request.toCommand()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    GoalResponse update(@PathVariable Long id, @Valid @RequestBody GoalRequest request,
            Authentication authentication) {
        return GoalResponse.from(service.update(id, authentication.getName(), request.toCommand()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    GoalResponse changeStatus(@PathVariable Long id, @Valid @RequestBody GoalStatusRequest request,
            Authentication authentication) {
        return GoalResponse.from(service.changeStatus(id, authentication.getName(), request.status()));
    }

    private boolean isReviewer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_REVIEWER"));
    }

    public record GoalRequest(
            @NotNull Long subjectId,
            @NotBlank @Size(max = 160) String title,
            @NotNull GoalType goalType,
            @NotNull GoalPriority priority,
            Instant plannedStart,
            @NotNull Instant dueAt,
            @Size(max = 1000) String notes,
            @Size(max = 500) String evidenceUrl,
            boolean portfolioVisible) {
        DegreeGoalService.GoalCommand toCommand() {
            return new DegreeGoalService.GoalCommand(subjectId, title, goalType, priority,
                    plannedStart, dueAt, notes == null ? "" : notes, evidenceUrl, portfolioVisible);
        }
    }

    public record GoalStatusRequest(@NotNull GoalStatus status) {
    }

    public record GoalResponse(
            Long id,
            Long subjectId,
            String subjectCode,
            String subjectName,
            String subjectColor,
            String ownerEmail,
            String title,
            GoalType goalType,
            GoalPriority priority,
            Instant plannedStart,
            Instant dueAt,
            String notes,
            String evidenceUrl,
            boolean portfolioVisible,
            GoalStatus status,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        static GoalResponse from(DegreeGoal goal) {
            return new GoalResponse(goal.getId(), goal.getSubject().getId(), goal.getSubject().getCode(),
                    goal.getSubject().getName(), goal.getSubject().getColor(), goal.getOwnerEmail(),
                    goal.getTitle(), goal.getGoalType(), goal.getPriority(), goal.getPlannedStart(),
                    goal.getDueAt(), goal.getNotes(), goal.getEvidenceUrl(), goal.isPortfolioVisible(),
                    goal.getStatus(), goal.getCompletedAt(), goal.getCreatedAt(), goal.getUpdatedAt(),
                    goal.getVersion());
        }
    }
}
