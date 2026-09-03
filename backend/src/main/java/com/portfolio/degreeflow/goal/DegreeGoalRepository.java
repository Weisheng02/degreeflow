package com.portfolio.degreeflow.goal;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DegreeGoalRepository extends JpaRepository<DegreeGoal, Long> {

    @EntityGraph(attributePaths = "subject")
    List<DegreeGoal> findByOwnerEmailOrderByDueAtAsc(String ownerEmail);

    @EntityGraph(attributePaths = "subject")
    List<DegreeGoal> findByOwnerEmailAndPortfolioVisibleTrueOrderByDueAtAsc(String ownerEmail);

    @EntityGraph(attributePaths = "subject")
    @Query("select goal from DegreeGoal goal where goal.id = :id and goal.ownerEmail = :ownerEmail")
    Optional<DegreeGoal> findOwnedWithSubject(@Param("id") Long id, @Param("ownerEmail") String ownerEmail);

    boolean existsByOwnerEmailAndGoalTypeAndStatusInAndPlannedStartLessThanAndDueAtGreaterThanAndIdNot(
            String ownerEmail,
            GoalType goalType,
            Collection<GoalStatus> statuses,
            Instant requestedEnd,
            Instant requestedStart,
            Long excludedId);

    long countByOwnerEmail(String ownerEmail);
}
