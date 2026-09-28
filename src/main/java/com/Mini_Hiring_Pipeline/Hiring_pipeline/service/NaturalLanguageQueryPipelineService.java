package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.LlmSecurityResponse;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.LlmSecurityResponse.SecurityStatus;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.SqlSecurityValidatorService.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NaturalLanguageQueryPipelineService {

    private static final Logger log = LoggerFactory.getLogger(NaturalLanguageQueryPipelineService.class);

    private final GeminiLlmSecurityService geminiSecurityService;
    private final SqlSecurityValidatorService sqlValidatorService;
    private final SafeQueryExecutionService safeQueryExecutionService;

    public NaturalLanguageQueryPipelineService(
            GeminiLlmSecurityService geminiSecurityService,
            SqlSecurityValidatorService sqlValidatorService,
            SafeQueryExecutionService safeQueryExecutionService) {
        this.geminiSecurityService = geminiSecurityService;
        this.sqlValidatorService = sqlValidatorService;
        this.safeQueryExecutionService = safeQueryExecutionService;
    }

    /**
     * Executes end-to-end secure Natural Language query pipeline with multi-layer verification.
     */
    public LlmSecurityResponse processQuery(String userQuery) {
        // Step 1: LLM Security Analysis & SQL Generation (Layer 1)
        LlmSecurityResponse llmResponse = geminiSecurityService.analyzeAndGenerateSql(userQuery);

        // If LLM classified query as mutation, out-of-context, malicious, or missing key -> return immediately
        if (llmResponse.getStatus() != SecurityStatus.APPROVED || llmResponse.getGeneratedSql() == null) {
            log.warn("Query '{}' not executable by LLM Security Layer. Status: {}, Reason: {}", 
                     userQuery, llmResponse.getStatus(), llmResponse.getRejectionReason());
            if (llmResponse.getRejectionReason() == null || llmResponse.getRejectionReason().isBlank()) {
                llmResponse.setRejectionReason(
                    llmResponse.getExplanation() != null && !llmResponse.getExplanation().isBlank()
                        ? llmResponse.getExplanation()
                        : "Query cannot be mapped to existing database schema."
                );
            }
            return llmResponse;
        }

        // Step 2: Java AST SQL Security Validation (Layer 2)
        ValidationResult validation = sqlValidatorService.validateAndSanitize(llmResponse.getGeneratedSql());
        if (!validation.isValid()) {
            log.warn("Generated SQL failed AST validation: {}. Error: {}", 
                     llmResponse.getGeneratedSql(), validation.errorMessage());
            llmResponse.setStatus(SecurityStatus.VALIDATION_FAILED);
            llmResponse.setRejectionReason("AST Security Validation Failed: " + validation.errorMessage());
            llmResponse.setGeneratedSql(null);
            return llmResponse;
        }

        // Step 3: Safe Read-Only Database Execution (Layer 3)
        try {
            List<Map<String, Object>> records = safeQueryExecutionService.executeReadOnlyQuery(validation.sanitizedSql());
            llmResponse.setGeneratedSql(validation.sanitizedSql());
            llmResponse.setQueryResults(records);
            return llmResponse;
        } catch (Exception e) {
            log.error("Database execution error for query: {}", validation.sanitizedSql(), e);
            llmResponse.setStatus(SecurityStatus.ERROR);
            llmResponse.setRejectionReason("Database execution error: " + e.getMessage());
            return llmResponse;
        }
    }
}
