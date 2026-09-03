package com.portfolio.degreeflow.subject;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/subjects")
public class StudySubjectController {

    public static final String DEMO_OWNER = "student@degreeflow.local";
    private final StudySubjectService service;

    public StudySubjectController(StudySubjectService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    List<SubjectResponse> list(Authentication authentication) {
        return service.listForStudent(authentication.getName()).stream().map(SubjectResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    SubjectResponse create(@Valid @RequestBody SubjectRequest request, Authentication authentication) {
        return SubjectResponse.from(service.create(authentication.getName(), request.toCommand()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request,
            Authentication authentication) {
        return SubjectResponse.from(service.update(id, authentication.getName(), request.toCommand()));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('STUDENT')")
    SubjectResponse archive(@PathVariable Long id, Authentication authentication) {
        return SubjectResponse.from(service.archive(id, authentication.getName()));
    }

    public record SubjectRequest(
            @NotBlank @Size(max = 24) String code,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 80) String semester,
            @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color) {
        StudySubjectService.SubjectCommand toCommand() {
            return new StudySubjectService.SubjectCommand(code, name, semester, color);
        }
    }

    public record SubjectResponse(Long id, String code, String name, String semester, String color,
            boolean active, long version) {
        static SubjectResponse from(StudySubject subject) {
            return new SubjectResponse(subject.getId(), subject.getCode(), subject.getName(),
                    subject.getSemester(), subject.getColor(), subject.isActive(), subject.getVersion());
        }
    }
}
