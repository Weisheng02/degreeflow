package com.portfolio.degreeflow.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_workspace")
public class StudentWorkspace {

    @Id
    @Column(name = "owner_email", nullable = false, length = 160)
    private String ownerEmail;

    protected StudentWorkspace() {
    }

    public StudentWorkspace(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }
}
