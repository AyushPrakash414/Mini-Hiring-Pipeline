package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.Candidate;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStageHistory;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateRepository;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateSearchResult;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateStageHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateStageHistoryRepository stageHistoryRepository;

    public CandidateService(CandidateRepository candidateRepository,
                            CandidateStageHistoryRepository stageHistoryRepository) {
        this.candidateRepository = candidateRepository;
        this.stageHistoryRepository = stageHistoryRepository;
    }

    /**
     * Atomically registers a new candidate and logs the initial audit history.
     */
    @Transactional
    public Candidate registerCandidate(String name, String email, String phone) {
        Candidate candidate = new Candidate(name, email, phone);
        candidate = candidateRepository.save(candidate);

        // Record initial application audit entry
        CandidateStageHistory initialAudit = new CandidateStageHistory(
                candidate.getId(),
                null,
                CandidateStage.APPLIED,
                "Candidate initial registration"
        );
        stageHistoryRepository.save(initialAudit);

        return candidate;
    }

    /**
     * Atomically advances or rejects a candidate to a new stage with audit record.
     */
    @Transactional
    public Candidate transitionStage(Long candidateId, CandidateStage targetStage, String reason) {
        Candidate candidate = candidateRepository.findByIdWithLock(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + candidateId));

        CandidateStage currentStage = candidate.getCurrentStage();

        // Enforce transition rules in business layer
        if (!currentStage.canTransitionTo(targetStage)) {
            throw new IllegalStateException(String.format(
                    "Invalid stage transition: cannot move from %s to %s",
                    currentStage, targetStage
            ));
        }

        // 1. Update candidate stage
        candidate.setCurrentStage(targetStage);
        candidate = candidateRepository.save(candidate);

        // 2. Append immutable audit history record
        CandidateStageHistory historyEntry = new CandidateStageHistory(
                candidate.getId(),
                currentStage,
                targetStage,
                reason
        );
        stageHistoryRepository.save(historyEntry);

        return candidate;
    }

    /**
     * Typo-tolerant fuzzy candidate search using pg_trgm.
     */
    @Transactional(readOnly = true)
    public List<CandidateSearchResult> searchCandidates(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return candidateRepository.searchByNameFuzzy(query.trim(), limit > 0 ? limit : 20);
    }

    /**
     * Retrieves full chronological progression for a candidate.
     */
    @Transactional(readOnly = true)
    public List<CandidateStageHistory> getCandidateHistory(Long candidateId) {
        return stageHistoryRepository.findByCandidateIdOrderByChangedAtAsc(candidateId);
    }

    /**
     * Retrieves candidate by ID.
     */
    @Transactional(readOnly = true)
    public Candidate getCandidateById(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + candidateId));
    }

    /**
     * Retrieves all candidates.
     */
    @Transactional(readOnly = true)
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    /**
     * Retrieves all candidates grouped by their current stage.
     * Guaranteed to contain all CandidateStage keys even when a stage has 0 candidates.
     */
    @Transactional(readOnly = true)
    public java.util.Map<CandidateStage, List<Candidate>> getCandidatesGroupedByStage() {
        java.util.Map<CandidateStage, List<Candidate>> grouped = new java.util.EnumMap<>(CandidateStage.class);
        for (CandidateStage stage : CandidateStage.values()) {
            grouped.put(stage, new java.util.ArrayList<>());
        }
        List<Candidate> all = candidateRepository.findAll();
        for (Candidate c : all) {
            grouped.get(c.getCurrentStage()).add(c);
        }
        return grouped;
    }
}
