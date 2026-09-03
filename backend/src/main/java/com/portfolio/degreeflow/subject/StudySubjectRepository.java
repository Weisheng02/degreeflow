package com.portfolio.degreeflow.subject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySubjectRepository extends JpaRepository<StudySubject, Long> {
    List<StudySubject> findByOwnerEmailAndActiveTrueOrderByCodeAsc(String ownerEmail);
    List<StudySubject> findByOwnerEmailOrderByCodeAsc(String ownerEmail);
    Optional<StudySubject> findByIdAndOwnerEmail(Long id, String ownerEmail);
    boolean existsByOwnerEmailAndCodeIgnoreCase(String ownerEmail, String code);
    boolean existsByOwnerEmailAndCodeIgnoreCaseAndIdNot(String ownerEmail, String code, Long id);
    long countByOwnerEmail(String ownerEmail);
}
