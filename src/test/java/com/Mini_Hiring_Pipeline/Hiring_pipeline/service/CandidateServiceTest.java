package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.Candidate;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStageHistory;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateRepository;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateStageHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateStageHistoryRepository stageHistoryRepository;

    @InjectMocks
    private CandidateService candidateService;

    private Candidate sampleCandidate;

    @BeforeEach
    void setUp() {
        sampleCandidate = new Candidate("Aarav Sharma", "aarav.sharma@example.com", "+91-9876543210");
        sampleCandidate.setId(101L);
        sampleCandidate.setCurrentStage(CandidateStage.APPLIED);
    }

    // ========================================================================
    // CANDIDATE CREATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Candidate creation initializes in APPLIED stage with stageStartedAt and audit history")
    void testCandidateCreation_Success() {
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            c.setId(101L);
            return c;
        });

        Candidate created = candidateService.registerCandidate("Aarav Sharma", "aarav.sharma@example.com", "+91-9876543210");

        assertNotNull(created);
        assertEquals(CandidateStage.APPLIED, created.getCurrentStage());
        assertNotNull(created.getStageStartedAt());
        assertNotNull(created.getCreatedAt());
        assertTrue(created.getTimeInCurrentStageSeconds() >= 0);

        ArgumentCaptor<CandidateStageHistory> historyCaptor = ArgumentCaptor.forClass(CandidateStageHistory.class);
        verify(stageHistoryRepository, times(1)).save(historyCaptor.capture());

        CandidateStageHistory initialHistory = historyCaptor.getValue();
        assertEquals(101L, initialHistory.getCandidateId());
        assertNull(initialHistory.getFromStage());
        assertEquals(CandidateStage.APPLIED, initialHistory.getToStage());
    }

    // ========================================================================
    // VALID STAGE PROGRESSION TESTS
    // ========================================================================

    @Test
    @DisplayName("Valid transition: APPLIED -> SCREENING")
    void testValidTransition_AppliedToScreening() {
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime prevStageStartedAt = sampleCandidate.getStageStartedAt();
        Candidate updated = candidateService.transitionStage(101L, CandidateStage.SCREENING, "Passed initial resume review");

        assertEquals(CandidateStage.SCREENING, updated.getCurrentStage());
        assertNotNull(updated.getStageStartedAt());

        ArgumentCaptor<CandidateStageHistory> historyCaptor = ArgumentCaptor.forClass(CandidateStageHistory.class);
        verify(stageHistoryRepository).save(historyCaptor.capture());
        CandidateStageHistory history = historyCaptor.getValue();
        assertEquals(CandidateStage.APPLIED, history.getFromStage());
        assertEquals(CandidateStage.SCREENING, history.getToStage());
        assertEquals("Passed initial resume review", history.getReason());
    }

    @Test
    @DisplayName("Valid transition: SCREENING -> INTERVIEW")
    void testValidTransition_ScreeningToInterview() {
        sampleCandidate.setCurrentStage(CandidateStage.SCREENING);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.INTERVIEW, "Scheduled technical round");
        assertEquals(CandidateStage.INTERVIEW, updated.getCurrentStage());
    }

    @Test
    @DisplayName("Valid transition: INTERVIEW -> OFFER")
    void testValidTransition_InterviewToOffer() {
        sampleCandidate.setCurrentStage(CandidateStage.INTERVIEW);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.OFFER, "Passed all interviews with strong hire rating");
        assertEquals(CandidateStage.OFFER, updated.getCurrentStage());
    }

    @Test
    @DisplayName("Valid transition: OFFER -> HIRED")
    void testValidTransition_OfferToHired() {
        sampleCandidate.setCurrentStage(CandidateStage.OFFER);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.HIRED, "Signed offer letter");
        assertEquals(CandidateStage.HIRED, updated.getCurrentStage());
    }

    // ========================================================================
    // REJECTION TESTS FROM ALL VALID NON-TERMINAL STAGES
    // ========================================================================

    @Test
    @DisplayName("Valid rejection: APPLIED -> REJECTED")
    void testValidRejection_AppliedToRejected() {
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.REJECTED, "Does not meet basic criteria");
        assertEquals(CandidateStage.REJECTED, updated.getCurrentStage());
    }

    @Test
    @DisplayName("Valid rejection: SCREENING -> REJECTED")
    void testValidRejection_ScreeningToRejected() {
        sampleCandidate.setCurrentStage(CandidateStage.SCREENING);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.REJECTED, "Failed screening assessment");
        assertEquals(CandidateStage.REJECTED, updated.getCurrentStage());
    }

    @Test
    @DisplayName("Valid rejection: INTERVIEW -> REJECTED")
    void testValidRejection_InterviewToRejected() {
        sampleCandidate.setCurrentStage(CandidateStage.INTERVIEW);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.REJECTED, "System design round rejected");
        assertEquals(CandidateStage.REJECTED, updated.getCurrentStage());
    }

    @Test
    @DisplayName("Valid rejection: OFFER -> REJECTED")
    void testValidRejection_OfferToRejected() {
        sampleCandidate.setCurrentStage(CandidateStage.OFFER);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        Candidate updated = candidateService.transitionStage(101L, CandidateStage.REJECTED, "Candidate declined offer");
        assertEquals(CandidateStage.REJECTED, updated.getCurrentStage());
    }

    // ========================================================================
    // INVALID STAGE TRANSITIONS & TERMINAL STATE TESTS
    // ========================================================================

    @Test
    @DisplayName("Invalid transition: APPLIED -> INTERVIEW (Skipping SCREENING must fail)")
    void testInvalidTransition_AppliedToInterview_Fails() {
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.INTERVIEW, "Attempting skip")
        );
        assertTrue(ex.getMessage().contains("Invalid stage transition"));
        verify(stageHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid transition: APPLIED -> OFFER (Skipping stages must fail)")
    void testInvalidTransition_AppliedToOffer_Fails() {
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.OFFER, "Direct offer")
        );
        verify(stageHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid transition: SCREENING -> OFFER (Skipping INTERVIEW must fail)")
    void testInvalidTransition_ScreeningToOffer_Fails() {
        sampleCandidate.setCurrentStage(CandidateStage.SCREENING);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.OFFER, "Skip interview")
        );
    }

    @Test
    @DisplayName("Invalid transition: INTERVIEW -> SCREENING (Backwards move must fail)")
    void testInvalidTransition_InterviewToScreening_Fails() {
        sampleCandidate.setCurrentStage(CandidateStage.INTERVIEW);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.SCREENING, "Backwards move")
        );
    }

    @Test
    @DisplayName("Invalid transition: OFFER -> SCREENING (Backwards move must fail)")
    void testInvalidTransition_OfferToScreening_Fails() {
        sampleCandidate.setCurrentStage(CandidateStage.OFFER);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.SCREENING, "Backwards move")
        );
    }

    @Test
    @DisplayName("Invalid transition: HIRED -> OFFER (HIRED is terminal, cannot change)")
    void testInvalidTransition_HiredToOffer_Fails() {
        sampleCandidate.setCurrentStage(CandidateStage.HIRED);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.OFFER, "Revert hired")
        );
    }

    @Test
    @DisplayName("Invalid transition: HIRED -> REJECTED (HIRED cannot be rejected)")
    void testInvalidTransition_HiredToRejected_Fails() {
        sampleCandidate.setCurrentStage(CandidateStage.HIRED);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.REJECTED, "Reject hired")
        );
    }

    @Test
    @DisplayName("Invalid transition: REJECTED -> SCREENING (REJECTED is terminal, cannot reopen)")
    void testInvalidTransition_RejectedToScreening_Fails() {
        sampleCandidate.setCurrentStage(CandidateStage.REJECTED);
        when(candidateRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCandidate));

        assertThrows(IllegalStateException.class, () ->
            candidateService.transitionStage(101L, CandidateStage.SCREENING, "Reopen candidate")
        );
    }

    // ========================================================================
    // PIPELINE GROUPING TEST
    // ========================================================================

    @Test
    @DisplayName("getCandidatesGroupedByStage returns all 6 stages even if some are empty")
    void testGetCandidatesGroupedByStage() {
        Candidate c1 = new Candidate("Alice", "alice@example.com", "+123");
        c1.setCurrentStage(CandidateStage.APPLIED);
        Candidate c2 = new Candidate("Bob", "bob@example.com", "+124");
        c2.setCurrentStage(CandidateStage.INTERVIEW);

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2));

        Map<CandidateStage, List<Candidate>> grouped = candidateService.getCandidatesGroupedByStage();

        assertNotNull(grouped);
        assertEquals(6, grouped.size());
        assertTrue(grouped.containsKey(CandidateStage.APPLIED));
        assertTrue(grouped.containsKey(CandidateStage.SCREENING));
        assertTrue(grouped.containsKey(CandidateStage.INTERVIEW));
        assertTrue(grouped.containsKey(CandidateStage.OFFER));
        assertTrue(grouped.containsKey(CandidateStage.HIRED));
        assertTrue(grouped.containsKey(CandidateStage.REJECTED));

        assertEquals(1, grouped.get(CandidateStage.APPLIED).size());
        assertEquals(0, grouped.get(CandidateStage.SCREENING).size());
        assertEquals(1, grouped.get(CandidateStage.INTERVIEW).size());
        assertEquals(0, grouped.get(CandidateStage.OFFER).size());
        assertEquals(0, grouped.get(CandidateStage.HIRED).size());
        assertEquals(0, grouped.get(CandidateStage.REJECTED).size());
    }
}
