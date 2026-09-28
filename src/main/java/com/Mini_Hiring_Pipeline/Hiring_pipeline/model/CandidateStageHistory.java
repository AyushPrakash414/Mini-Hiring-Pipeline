package com.Mini_Hiring_Pipeline.Hiring_pipeline.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "candidate_stage_history")
public class CandidateStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.type.PostgreSQLEnumJdbcType.class)
    @Column(name = "from_stage")
    private CandidateStage fromStage;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.type.PostgreSQLEnumJdbcType.class)
    @Column(name = "to_stage", nullable = false)
    private CandidateStage toStage;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    public CandidateStageHistory() {
    }

    public CandidateStageHistory(Long candidateId, CandidateStage fromStage, CandidateStage toStage, String reason) {
        this.candidateId = candidateId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.reason = reason;
        this.changedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public CandidateStage getFromStage() {
        return fromStage;
    }

    public void setFromStage(CandidateStage fromStage) {
        this.fromStage = fromStage;
    }

    public CandidateStage getToStage() {
        return toStage;
    }

    public void setToStage(CandidateStage toStage) {
        this.toStage = toStage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(OffsetDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
