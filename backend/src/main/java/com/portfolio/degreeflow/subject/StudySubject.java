package com.portfolio.degreeflow.subject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "study_subject")
public class StudySubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_email", nullable = false, length = 160)
    private String ownerEmail;

    @Column(nullable = false, length = 24)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String semester;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    protected StudySubject() {
    }

    public StudySubject(String ownerEmail, String code, String name, String semester, String color) {
        this.ownerEmail = ownerEmail;
        this.code = code;
        this.name = name;
        this.semester = semester;
        this.color = color;
    }

    public void update(String code, String name, String semester, String color) {
        this.code = code;
        this.name = name;
        this.semester = semester;
        this.color = color;
    }

    public void archive() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSemester() { return semester; }
    public String getColor() { return color; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
