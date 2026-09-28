package com.Mini_Hiring_Pipeline.Hiring_pipeline.controller;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.Candidate;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStageHistory;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.LlmSecurityResponse;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.repository.CandidateSearchResult;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.CandidateService;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.NaturalLanguageQueryPipelineService;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.OpenNlpQueryRouterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class CandidateController {

    private final CandidateService candidateService;
    private final OpenNlpQueryRouterService queryRouterService;
    private final NaturalLanguageQueryPipelineService nlQueryPipelineService;

    public CandidateController(
            CandidateService candidateService, 
            OpenNlpQueryRouterService queryRouterService,
            NaturalLanguageQueryPipelineService nlQueryPipelineService) {
        this.candidateService = candidateService;
        this.queryRouterService = queryRouterService;
        this.nlQueryPipelineService = nlQueryPipelineService;
    }

    /**
     * Route and classify search query using Apache OpenNLP POS Tagger.
     */
    @GetMapping("/route-query")
    public ResponseEntity<QueryRouteResponse> routeQuery(@RequestParam("query") String query) {
        QueryRouteResponse response = queryRouterService.routeQuery(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Process Natural Language Query through multi-layered security and read-only execution.
     */
    @PostMapping("/nl-query")
    public ResponseEntity<LlmSecurityResponse> processNlQueryPost(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "");
        LlmSecurityResponse response = nlQueryPipelineService.processQuery(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nl-query")
    public ResponseEntity<LlmSecurityResponse> processNlQueryGet(@RequestParam("query") String query) {
        LlmSecurityResponse response = nlQueryPipelineService.processQuery(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Fuzzy search candidates by name with pg_trgm similarity score.
     */
    @GetMapping("/search")
    public ResponseEntity<List<CandidateSearchResult>> searchCandidates(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<CandidateSearchResult> results = candidateService.searchCandidates(query, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Get candidate details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Candidate> getCandidateById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(candidateService.getCandidateById(id));
    }

    /**
     * Get candidate stage history timeline.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<CandidateStageHistory>> getCandidateHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(candidateService.getCandidateHistory(id));
    }

    /**
     * Get all candidates list.
     */
    @GetMapping
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.getAllCandidates());
    }

    /**
     * Get all candidates grouped by pipeline stage.
     */
    @GetMapping("/pipeline")
    public ResponseEntity<Map<CandidateStage, List<Candidate>>> getPipelineGrouped() {
        return ResponseEntity.ok(candidateService.getCandidatesGroupedByStage());
    }

    /**
     * Register a new candidate.
     */
    @PostMapping
    public ResponseEntity<Candidate> registerCandidate(
            @jakarta.validation.Valid @RequestBody com.Mini_Hiring_Pipeline.Hiring_pipeline.dto.CandidateCreateRequest request) {
        Candidate created = candidateService.registerCandidate(
                request.getName(),
                request.getEmail(),
                request.getPhone()
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    /**
     * Advance or reject candidate stage.
     */
    @PostMapping("/{id}/transition")
    public ResponseEntity<Candidate> transitionStage(
            @PathVariable("id") Long id,
            @jakarta.validation.Valid @RequestBody com.Mini_Hiring_Pipeline.Hiring_pipeline.dto.StageTransitionRequest request) {
        Candidate updated = candidateService.transitionStage(
                id,
                request.getTargetStage(),
                request.getReason()
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * Quick demo seed helper for initial testing.
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, String>> seedSampleData() {
        if (candidateService.getAllCandidates().isEmpty()) {
            Candidate c1 = candidateService.registerCandidate("Ayush Prakash Tiwari", "ayush.tiwari@example.com", "+91-9876543210");
            Candidate c2 = candidateService.registerCandidate("Ayush Kumar", "ayush.kumar@example.com", "+91-9876543211");
            Candidate c3 = candidateService.registerCandidate("Ankit Sharma", "ankit.sharma@example.com", "+91-9876543212");
            Candidate c4 = candidateService.registerCandidate("Priya Sharma", "priya.sharma@example.com", "+91-9876543213");

            // Progress Priya Sharma to OFFER
            candidateService.transitionStage(c4.getId(), CandidateStage.SCREENING, "Shortlisted based on portfolio");
            candidateService.transitionStage(c4.getId(), CandidateStage.INTERVIEW, "Passed HR & Technical round 1");
            candidateService.transitionStage(c4.getId(), CandidateStage.OFFER, "Cleared system design round");

            // Progress Ankit Sharma to REJECTED
            candidateService.transitionStage(c3.getId(), CandidateStage.SCREENING, "Initial phone screening");
            candidateService.transitionStage(c3.getId(), CandidateStage.REJECTED, "Skills mismatched for backend role");

            return ResponseEntity.ok(Map.of("message", "Sample candidates seeded successfully!"));
        }
        return ResponseEntity.ok(Map.of("message", "Candidates already exist in database."));
    }
}
