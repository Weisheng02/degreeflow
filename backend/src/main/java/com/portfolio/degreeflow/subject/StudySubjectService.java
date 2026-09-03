package com.portfolio.degreeflow.subject;

import java.util.List;
import java.util.regex.Pattern;

import com.portfolio.degreeflow.common.ConflictException;
import com.portfolio.degreeflow.common.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StudySubjectService {

    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private final StudySubjectRepository repository;

    public StudySubjectService(StudySubjectRepository repository) {
        this.repository = repository;
    }

    public List<StudySubject> listForStudent(String ownerEmail) {
        return repository.findByOwnerEmailAndActiveTrueOrderByCodeAsc(ownerEmail);
    }

    @Transactional
    public StudySubject create(String ownerEmail, SubjectCommand command) {
        validate(command);
        String code = command.code().trim().toUpperCase();
        if (repository.existsByOwnerEmailAndCodeIgnoreCase(ownerEmail, code)) {
            throw new ConflictException("A subject with this code already exists");
        }
        return repository.save(new StudySubject(ownerEmail, code, command.name().trim(),
                command.semester().trim(), command.color()));
    }

    @Transactional
    public StudySubject update(Long id, String ownerEmail, SubjectCommand command) {
        validate(command);
        StudySubject subject = findOwned(id, ownerEmail);
        String code = command.code().trim().toUpperCase();
        if (repository.existsByOwnerEmailAndCodeIgnoreCaseAndIdNot(ownerEmail, code, id)) {
            throw new ConflictException("A subject with this code already exists");
        }
        subject.update(code, command.name().trim(), command.semester().trim(), command.color());
        return subject;
    }

    @Transactional
    public StudySubject archive(Long id, String ownerEmail) {
        StudySubject subject = findOwned(id, ownerEmail);
        subject.archive();
        return subject;
    }

    StudySubject findOwned(Long id, String ownerEmail) {
        return repository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private void validate(SubjectCommand command) {
        if (!COLOR.matcher(command.color()).matches()) {
            throw new IllegalArgumentException("Color must be a six-digit hex value");
        }
    }

    public record SubjectCommand(String code, String name, String semester, String color) {
    }
}
