package com.Mini_Hiring_Pipeline.Hiring_pipeline.repository;

import java.time.Instant;

public interface CandidateSearchResult {
    Long getId();
    String getName();
    String getEmail();
    String getPhone();
    String getCurrentStage();
    Instant getCreatedAt();
    Double getSimilarityScore();
}
