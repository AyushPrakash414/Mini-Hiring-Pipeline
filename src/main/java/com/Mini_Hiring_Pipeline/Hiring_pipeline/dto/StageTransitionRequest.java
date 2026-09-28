package com.Mini_Hiring_Pipeline.Hiring_pipeline.dto;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StageTransitionRequest {

    @NotNull(message = "Target stage is required")
    @JsonProperty("targetStage")
    @JsonAlias({"stage", "targetStage"})
    private CandidateStage targetStage;

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    public StageTransitionRequest() {
    }

    public StageTransitionRequest(CandidateStage targetStage, String reason) {
        this.targetStage = targetStage;
        this.reason = reason;
    }

    public CandidateStage getTargetStage() {
        return targetStage;
    }

    public void setTargetStage(CandidateStage targetStage) {
        this.targetStage = targetStage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
