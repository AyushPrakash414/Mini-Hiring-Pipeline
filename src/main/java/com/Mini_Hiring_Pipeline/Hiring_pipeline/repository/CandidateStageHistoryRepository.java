package com.Mini_Hiring_Pipeline.Hiring_pipeline.repository;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateStageHistoryRepository extends JpaRepository<CandidateStageHistory, Long> {

    List<CandidateStageHistory> findByCandidateIdOrderByChangedAtAsc(Long candidateId);

    List<CandidateStageHistory> findByCandidateIdOrderByChangedAtDesc(Long candidateId);
}
