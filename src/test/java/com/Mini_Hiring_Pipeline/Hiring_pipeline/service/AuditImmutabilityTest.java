package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.Candidate;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStageHistory;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateRepository;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateStageHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditImmutabilityTest {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CandidateStageHistoryRepository historyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    @DisplayName("Audit History records are appended on registration and transitions")
    void testAuditHistoryAppended() {
        String testEmail = "audit.test." + System.currentTimeMillis() + "@example.com";
        Candidate candidate = candidateService.registerCandidate("Audit Candidate", testEmail, "+91-9999999999");
        assertNotNull(candidate.getId());

        List<CandidateStageHistory> initialHistory = historyRepository.findByCandidateIdOrderByChangedAtAsc(candidate.getId());
        assertEquals(1, initialHistory.size(), "Initial history must have exactly 1 record");
        assertEquals(CandidateStage.APPLIED, initialHistory.get(0).getToStage());
        assertNull(initialHistory.get(0).getFromStage());

        // Advance to SCREENING
        candidateService.transitionStage(candidate.getId(), CandidateStage.SCREENING, "Passed screening review");
        List<CandidateStageHistory> step2History = historyRepository.findByCandidateIdOrderByChangedAtAsc(candidate.getId());
        assertEquals(2, step2History.size(), "History must contain 2 events after progression");
        assertEquals(CandidateStage.APPLIED, step2History.get(1).getFromStage());
        assertEquals(CandidateStage.SCREENING, step2History.get(1).getToStage());

        // Advance to INTERVIEW
        candidateService.transitionStage(candidate.getId(), CandidateStage.INTERVIEW, "Passed phone round");
        List<CandidateStageHistory> step3History = historyRepository.findByCandidateIdOrderByChangedAtAsc(candidate.getId());
        assertEquals(3, step3History.size(), "History must contain 3 events");

        Long historyId = step3History.get(0).getId();

        // Verify Database Immutability Trigger prevents UPDATE
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update("UPDATE candidate_stage_history SET reason = 'tampered' WHERE id = ?", historyId);
        }, "Updating candidate_stage_history must be rejected by database immutability trigger");

        // Verify Database Immutability Trigger prevents DELETE
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update("DELETE FROM candidate_stage_history WHERE id = ?", historyId);
        }, "Deleting candidate_stage_history must be rejected by database immutability trigger");
    }
}
