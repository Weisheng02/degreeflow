package com.portfolio.degreeflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.portfolio.degreeflow.goal.DegreeGoalRepository;
import com.portfolio.degreeflow.subject.StudySubject;
import com.portfolio.degreeflow.subject.StudySubjectRepository;
import com.portfolio.degreeflow.workspace.StudentWorkspace;
import com.portfolio.degreeflow.workspace.StudentWorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DegreeFlowApiIntegrationTests {

    private static final String STUDENT = "student@degreeflow.local";
    private static final String STUDENT_PASSWORD = "Student123!";
    private static final String REVIEWER = "reviewer@degreeflow.local";
    private static final String REVIEWER_PASSWORD = "Reviewer123!";
    private static final String OTHER = "other@degreeflow.local";
    private static final String OTHER_PASSWORD = "Other123!";

    @Autowired MockMvc mockMvc;
    @Autowired DegreeGoalRepository goalRepository;
    @Autowired StudySubjectRepository subjectRepository;
    @Autowired StudentWorkspaceRepository workspaceRepository;

    private StudySubject studentSubject;
    private StudySubject otherSubject;

    @BeforeEach
    void resetData() {
        goalRepository.deleteAll();
        subjectRepository.deleteAll();
        workspaceRepository.deleteAll();
        workspaceRepository.save(new StudentWorkspace(STUDENT));
        workspaceRepository.save(new StudentWorkspace(OTHER));
        studentSubject = subjectRepository.save(new StudySubject(
                STUDENT, "SE", "Software Engineering", "Semester 1", "#EC5B35"));
        otherSubject = subjectRepository.save(new StudySubject(
                OTHER, "WEB", "Web Development", "Semester 1", "#326B5A"));
    }

    @Test
    void authenticationReturnsOnlyApplicationRoles() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic(STUDENT, STUDENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(STUDENT))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));

        mockMvc.perform(get("/api/auth/me").with(httpBasic(REVIEWER, REVIEWER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("REVIEWER"));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/goals")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/subjects")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanCreateUpdateAndArchiveSubject() throws Exception {
        String created = mockMvc.perform(post("/api/subjects")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectPayload("FYP", "Final Year Project", "Semester 4", "#6B5FB5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("FYP"))
                .andReturn().getResponse().getContentAsString();
        long id = extractId(created);

        mockMvc.perform(put("/api/subjects/{id}", id)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectPayload("FYP2", "FYP Preparation", "Next Semester", "#445566")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FYP Preparation"));

        mockMvc.perform(patch("/api/subjects/{id}/archive", id)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void duplicateSubjectCodeIsRejectedPerOwner() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectPayload("se", "Duplicate", "Semester 2", "#112233")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void reviewerCannotReadSubjectCatalogueOrWrite() throws Exception {
        mockMvc.perform(get("/api/subjects").with(httpBasic(REVIEWER, REVIEWER_PASSWORD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/subjects")
                        .with(httpBasic(REVIEWER, REVIEWER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subjectPayload("FYP", "Final Year Project", "Semester 4", "#112233")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanCreateAndUpdateGoal() throws Exception {
        String created = createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "ASSIGNMENT", "Write requirements", 48, 72, false, null);
        long id = extractId(created);

        mockMvc.perform(put("/api/goals/{id}", id)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "PROJECT_MILESTONE", "Refine FYP scope",
                                72, 120, true, "https://github.com/example/project")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Refine FYP scope"))
                .andExpect(jsonPath("$.portfolioVisible").value(true));
    }

    @Test
    void goalsAreOwnerScoped() throws Exception {
        createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "ASSIGNMENT", "Student goal", 24, 48, false, null);
        createGoal(OTHER, OTHER_PASSWORD, otherSubject.getId(),
                "ASSIGNMENT", "Other goal", 48, 72, false, null);

        mockMvc.perform(get("/api/goals").with(httpBasic(STUDENT, STUDENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Student goal"));
    }

    @Test
    void studentCannotUpdateAnotherStudentsGoal() throws Exception {
        long otherGoalId = extractId(createGoal(OTHER, OTHER_PASSWORD, otherSubject.getId(),
                "ASSIGNMENT", "Other goal", 48, 72, false, null));

        mockMvc.perform(put("/api/goals/{id}", otherGoalId)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "ASSIGNMENT", "Takeover", 72, 96, false, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reviewerSeesOnlyPortfolioVisibleGoalsAndCannotWrite() throws Exception {
        createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "ASSIGNMENT", "Private assignment", 24, 48, false, null);
        createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "PORTFOLIO", "Public case study", 48, 72, true, "https://example.com/demo");

        mockMvc.perform(get("/api/goals").with(httpBasic(REVIEWER, REVIEWER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Public case study"));

        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(REVIEWER, REVIEWER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "PORTFOLIO", "Reviewer write", 72, 96, true, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void legalGoalTransitionsAreApplied() throws Exception {
        long id = extractId(createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "ASSIGNMENT", "Complete report", 24, 48, false, null));

        mockMvc.perform(patch("/api/goals/{id}/status", id)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/goals/{id}/status", id)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void terminalGoalCannotTransitionOrBeEdited() throws Exception {
        long id = extractId(createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "ASSIGNMENT", "Complete report", 24, 48, false, null));
        changeStatus(id, "COMPLETED").andExpect(status().isOk());

        changeStatus(id, "IN_PROGRESS")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(put("/api/goals/{id}", id)
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "ASSIGNMENT", "Edited", 72, 96, false, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingActiveStudySessionIsRejected() throws Exception {
        createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "STUDY_SESSION", "Graphs revision", 24, 27, false, null);
        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "STUDY_SESSION", "Testing revision",
                                26, 28, false, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This study session overlaps another active study session"));
    }

    @Test
    void overlappingAssignmentsAreAllowed() throws Exception {
        createGoal(STUDENT, STUDENT_PASSWORD, studentSubject.getId(),
                "ASSIGNMENT", "Assignment one", 24, 48, false, null);
        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "ASSIGNMENT", "Assignment two",
                                24, 48, false, null)))
                .andExpect(status().isCreated());
    }

    @Test
    void studySessionAndTimeRangeValidationAreEnforced() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayloadWithoutStart(studentSubject.getId(), "STUDY_SESSION", "Missing start", 48)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "STUDY_SESSION", "Invalid range",
                                48, 24, false, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evidenceAndPortfolioVisibilityValidationAreEnforced() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "ASSIGNMENT", "Private type",
                                24, 48, true, "file:///tmp/report.pdf")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "PORTFOLIO", "Bad link",
                                24, 48, true, "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archivedSubjectCannotReceiveNewGoals() throws Exception {
        mockMvc.perform(patch("/api/subjects/{id}/archive", studentSubject.getId())
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/goals")
                        .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(studentSubject.getId(), "ASSIGNMENT", "Archived subject goal",
                                24, 48, false, null)))
                .andExpect(status().isNotFound());
    }

    private String createGoal(String username, String password, long subjectId, String type, String title,
            long startHours, long dueHours, boolean visible, String evidenceUrl) throws Exception {
        return mockMvc.perform(post("/api/goals")
                        .with(httpBasic(username, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalPayload(subjectId, type, title, startHours, dueHours, visible, evidenceUrl)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(long id, String statusValue) throws Exception {
        return mockMvc.perform(patch("/api/goals/{id}/status", id)
                .with(httpBasic(STUDENT, STUDENT_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"%s\"}".formatted(statusValue)));
    }

    private String goalPayload(long subjectId, String type, String title, long startHours, long dueHours,
            boolean visible, String evidenceUrl) {
        Instant base = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        String start = type.equals("STUDY_SESSION") ? "\"%s\"".formatted(base.plus(startHours, ChronoUnit.HOURS)) : "null";
        String evidence = evidenceUrl == null ? "null" : "\"%s\"".formatted(evidenceUrl);
        return """
                {
                  "subjectId": %d,
                  "title": "%s",
                  "goalType": "%s",
                  "priority": "HIGH",
                  "plannedStart": %s,
                  "dueAt": "%s",
                  "notes": "Integration test evidence",
                  "evidenceUrl": %s,
                  "portfolioVisible": %s
                }
                """.formatted(subjectId, title, type, start, base.plus(dueHours, ChronoUnit.HOURS), evidence, visible);
    }

    private String goalPayloadWithoutStart(long subjectId, String type, String title, long dueHours) {
        Instant due = Instant.now().plus(dueHours, ChronoUnit.HOURS);
        return """
                {
                  "subjectId": %d,
                  "title": "%s",
                  "goalType": "%s",
                  "priority": "MEDIUM",
                  "plannedStart": null,
                  "dueAt": "%s",
                  "notes": "Missing planned start",
                  "evidenceUrl": null,
                  "portfolioVisible": false
                }
                """.formatted(subjectId, title, type, due);
    }

    private String subjectPayload(String code, String name, String semester, String color) {
        return """
                {"code":"%s","name":"%s","semester":"%s","color":"%s"}
                """.formatted(code, name, semester, color);
    }

    private long extractId(String json) {
        return Long.parseLong(json.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
