package com.portfolio.degreeflow.workspace;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface StudentWorkspaceRepository extends JpaRepository<StudentWorkspace, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentWorkspace> findByOwnerEmail(String ownerEmail);
}
