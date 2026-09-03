package com.portfolio.degreeflow.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.portfolio.degreeflow.goal.DegreeGoal;
import com.portfolio.degreeflow.goal.DegreeGoalRepository;
import com.portfolio.degreeflow.goal.GoalPriority;
import com.portfolio.degreeflow.goal.GoalStatus;
import com.portfolio.degreeflow.goal.GoalType;
import com.portfolio.degreeflow.subject.StudySubject;
import com.portfolio.degreeflow.subject.StudySubjectController;
import com.portfolio.degreeflow.subject.StudySubjectRepository;
import com.portfolio.degreeflow.workspace.StudentWorkspace;
import com.portfolio.degreeflow.workspace.StudentWorkspaceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {

    @Bean
    CommandLineRunner seedDegreeFlow(StudentWorkspaceRepository workspaceRepository,
            StudySubjectRepository subjectRepository, DegreeGoalRepository goalRepository) {
        return args -> {
            String owner = StudySubjectController.DEMO_OWNER;
            workspaceRepository.findById(owner)
                    .orElseGet(() -> workspaceRepository.save(new StudentWorkspace(owner)));

            if (subjectRepository.countByOwnerEmail(owner) > 0) {
                return;
            }

            StudySubject softwareEngineering = subjectRepository.save(new StudySubject(
                    owner, "SE", "Software Engineering", "Current Semester", "#EC5B35"));
            StudySubject dataStructures = subjectRepository.save(new StudySubject(
                    owner, "DSA", "Data Structures & Algorithms", "Current Semester", "#326B5A"));
            StudySubject fyp = subjectRepository.save(new StudySubject(
                    owner, "FYP", "Final Year Project Preparation", "Next Semester", "#6B5FB5"));
            StudySubject portfolio = subjectRepository.save(new StudySubject(
                    owner, "CAREER", "Career & Portfolio", "Graduate Preparation", "#B0702F"));

            Instant now = Instant.now();
            goalRepository.save(new DegreeGoal(dataStructures, owner,
                    "Revise tree and graph operations", GoalType.STUDY_SESSION, GoalPriority.HIGH,
                    now.plus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS).plus(90, ChronoUnit.MINUTES),
                    "Review traversal, balancing and complexity before the next practical.", null, false, now));
            goalRepository.save(new DegreeGoal(softwareEngineering, owner,
                    "Complete requirements and UI evidence", GoalType.ASSIGNMENT, GoalPriority.HIGH,
                    null, now.plus(7, ChronoUnit.DAYS),
                    "Match the implementation evidence to the lecturer requirements.", null, false, now));

            DegreeGoal fypGoal = new DegreeGoal(fyp, owner,
                    "Interview one potential FYP stakeholder", GoalType.PROJECT_MILESTONE, GoalPriority.HIGH,
                    null, now.plus(14, ChronoUnit.DAYS),
                    "Confirm a real workflow and record only evidence that actually exists.", null, true, now);
            fypGoal.moveTo(GoalStatus.IN_PROGRESS, now);
            goalRepository.save(fypGoal);

            DegreeGoal portfolioGoal = new DegreeGoal(portfolio, owner,
                    "Publish one tested full-stack case study", GoalType.PORTFOLIO, GoalPriority.MEDIUM,
                    null, now.plus(30, ChronoUnit.DAYS),
                    "Include architecture, tests, deployment evidence and personal contribution.",
                    "https://github.com/Weisheng02/degreeflow", true, now);
            portfolioGoal.moveTo(GoalStatus.COMPLETED, now);
            goalRepository.save(portfolioGoal);
        };
    }
}
