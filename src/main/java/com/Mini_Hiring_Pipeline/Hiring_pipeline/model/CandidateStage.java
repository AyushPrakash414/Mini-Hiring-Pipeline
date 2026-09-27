package com.Mini_Hiring_Pipeline.Hiring_pipeline.model;

public enum CandidateStage {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED;

    public boolean isTerminal() {
        return this == HIRED || this == REJECTED;
    }

    public boolean canTransitionTo(CandidateStage target) {
        if (target == null || isTerminal()) {
            return false;
        }
        if (target == REJECTED) {
            return true;
        }
        return switch (this) {
            case APPLIED -> target == SCREENING;
            case SCREENING -> target == INTERVIEW;
            case INTERVIEW -> target == OFFER;
            case OFFER -> target == HIRED;
            default -> false;
        };
    }
}
