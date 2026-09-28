package com.Mini_Hiring_Pipeline.Hiring_pipeline.controller;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.dto.CandidateCreateRequest;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.dto.StageTransitionRequest;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.exception.GlobalExceptionHandler;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.Candidate;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.CandidateService;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.NaturalLanguageQueryPipelineService;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.OpenNlpQueryRouterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CandidateControllerIntegrationTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private CandidateService candidateService;

    @Mock
    private OpenNlpQueryRouterService queryRouterService;

    @Mock
    private NaturalLanguageQueryPipelineService nlQueryPipelineService;

    @InjectMocks
    private CandidateController candidateController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(candidateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/candidates creates candidate and returns 201 Created")
    void testRegisterCandidate_Success() throws Exception {
        CandidateCreateRequest req = new CandidateCreateRequest("Priya Sharma", "priya.sharma@example.com", "+91-9876543213");
        Candidate c = new Candidate("Priya Sharma", "priya.sharma@example.com", "+91-9876543213");
        c.setId(1L);

        when(candidateService.registerCandidate("Priya Sharma", "priya.sharma@example.com", "+91-9876543213")).thenReturn(c);

        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Priya Sharma")))
                .andExpect(jsonPath("$.email", is("priya.sharma@example.com")))
                .andExpect(jsonPath("$.currentStage", is("APPLIED")));
    }

    @Test
    @DisplayName("POST /api/candidates with blank name returns 400 Bad Request")
    void testRegisterCandidate_BlankName_Fails() throws Exception {
        CandidateCreateRequest req = new CandidateCreateRequest("", "priya@example.com", "+91-9876543213");

        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST /api/candidates with invalid email format returns 400 Bad Request")
    void testRegisterCandidate_InvalidEmail_Fails() throws Exception {
        CandidateCreateRequest req = new CandidateCreateRequest("Priya", "invalid-email-address", "+91-9876543213");

        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid email format")));
    }

    @Test
    @DisplayName("POST /api/candidates with duplicate email returns 409 Conflict")
    void testRegisterCandidate_DuplicateEmail_Returns409() throws Exception {
        CandidateCreateRequest req = new CandidateCreateRequest("Priya Sharma", "priya.sharma@example.com", "+91-9876543213");

        when(candidateService.registerCandidate(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uq_candidates_email"));

        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("email address already exists")));
    }

    @Test
    @DisplayName("POST /api/candidates/{id}/transition valid stage returns 200 OK")
    void testTransitionStage_Success() throws Exception {
        StageTransitionRequest req = new StageTransitionRequest(CandidateStage.SCREENING, "Resume approved");
        Candidate c = new Candidate("Priya Sharma", "priya.sharma@example.com", "+91-9876543213");
        c.setId(1L);
        c.setCurrentStage(CandidateStage.SCREENING);

        when(candidateService.transitionStage(eq(1L), eq(CandidateStage.SCREENING), eq("Resume approved"))).thenReturn(c);

        mockMvc.perform(post("/api/candidates/1/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStage", is("SCREENING")));
    }

    @Test
    @DisplayName("POST /api/candidates/{id}/transition invalid transition returns 409 Conflict")
    void testTransitionStage_InvalidMove_Returns409() throws Exception {
        StageTransitionRequest req = new StageTransitionRequest(CandidateStage.HIRED, "Skip directly");

        when(candidateService.transitionStage(eq(1L), eq(CandidateStage.HIRED), any()))
                .thenThrow(new IllegalStateException("Invalid stage transition: cannot move from APPLIED to HIRED"));

        mockMvc.perform(post("/api/candidates/1/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("Invalid stage transition")));
    }

    @Test
    @DisplayName("POST /api/candidates/{id}/transition nonexistent candidate returns 404 Not Found")
    void testTransitionStage_NotFound_Returns404() throws Exception {
        StageTransitionRequest req = new StageTransitionRequest(CandidateStage.SCREENING, "Notes");

        when(candidateService.transitionStage(eq(999L), any(), any()))
                .thenThrow(new IllegalArgumentException("Candidate not found with id: 999"));

        mockMvc.perform(post("/api/candidates/999/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("GET /api/candidates/pipeline returns grouped map of all 6 stages")
    void testGetPipelineGrouped_Success() throws Exception {
        Map<CandidateStage, List<Candidate>> grouped = new EnumMap<>(CandidateStage.class);
        for (CandidateStage s : CandidateStage.values()) {
            grouped.put(s, List.of());
        }

        when(candidateService.getCandidatesGroupedByStage()).thenReturn(grouped);

        mockMvc.perform(get("/api/candidates/pipeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.APPLIED", notNullValue()))
                .andExpect(jsonPath("$.SCREENING", notNullValue()))
                .andExpect(jsonPath("$.INTERVIEW", notNullValue()))
                .andExpect(jsonPath("$.OFFER", notNullValue()))
                .andExpect(jsonPath("$.HIRED", notNullValue()))
                .andExpect(jsonPath("$.REJECTED", notNullValue()));
    }
}
