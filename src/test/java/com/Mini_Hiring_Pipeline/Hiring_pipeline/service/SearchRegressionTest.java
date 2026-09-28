package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse.Intent;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.SqlSecurityValidatorService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchRegressionTest {

    private OpenNlpQueryRouterService routerService;
    private SqlSecurityValidatorService sqlValidator;

    @BeforeEach
    void setUp() {
        routerService = new OpenNlpQueryRouterService();
        routerService.init();
        sqlValidator = new SqlSecurityValidatorService();
    }

    // ========================================================================
    // 1. NAME SEARCH REGRESSION TESTS
    // ========================================================================

    @Test
    @DisplayName("Candidate name queries routed to NAME_SEARCH")
    void testNameSearchQueries() {
        assertEquals(Intent.NAME_SEARCH, routerService.routeQuery("Priya Sharma").getIntent());
        assertEquals(Intent.NAME_SEARCH, routerService.routeQuery("Priya Sharmma").getIntent());
        assertEquals(Intent.NAME_SEARCH, routerService.routeQuery("ayusf").getIntent());
        assertEquals(Intent.NAME_SEARCH, routerService.routeQuery("Misra").getIntent());
        assertEquals(Intent.NAME_SEARCH, routerService.routeQuery("will smith").getIntent());
        assertEquals(Intent.NAME_SEARCH, routerService.routeQuery("John Doe").getIntent());
    }

    // ========================================================================
    // 2. NATURAL LANGUAGE INTENT REGRESSION TESTS
    // ========================================================================

    @Test
    @DisplayName("Stage, Duration, History, Exclusion, and Combined queries routed to NATURAL_LANGUAGE")
    void testNaturalLanguageIntentQueries() {
        // Current Stage
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Who is in Interview right now?").getIntent());
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Show candidates in Screening").getIntent());

        // Duration
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Who has been stuck in Screening for more than a week?").getIntent());

        // History
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Who moved to Interview since Monday?").getIntent());
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Who reached Offer but didn't get hired?").getIntent());

        // Exclusion
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Everyone except rejected candidates").getIntent());

        // Combined
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("Find candidates currently in Interview who entered after Monday and have been there for more than 3 days.").getIntent());

        // Read-only with word 'delete' in context
        assertEquals(Intent.NATURAL_LANGUAGE, routerService.routeQuery("I don't want you to delete anything. Show me candidates who would remain if rejected candidates were excluded.").getIntent());
    }

    // ========================================================================
    // 3. SQL AST SECURITY & READ-ONLY ENFORCEMENT REGRESSION TESTS
    // ========================================================================

    @Test
    @DisplayName("AST validator permits valid read-only SELECT and joins on candidates and history")
    void testValidSelectQueriesPassAstValidation() {
        ValidationResult stageQuery = sqlValidator.validateAndSanitize(
            "SELECT id, name, email, phone, current_stage FROM candidates WHERE current_stage = 'INTERVIEW' LIMIT 50"
        );
        assertTrue(stageQuery.isValid());

        ValidationResult durationQuery = sqlValidator.validateAndSanitize(
            "SELECT id, name, email, phone, current_stage, stage_started_at FROM candidates WHERE current_stage = 'SCREENING' AND stage_started_at < NOW() - INTERVAL '7 days' LIMIT 50"
        );
        assertTrue(durationQuery.isValid());

        ValidationResult historyJoinQuery = sqlValidator.validateAndSanitize(
            "SELECT c.id, c.name, h.to_stage, h.changed_at FROM candidates c JOIN candidate_stage_history h ON c.id = h.candidate_id WHERE h.to_stage = 'INTERVIEW' LIMIT 50"
        );
        assertTrue(historyJoinQuery.isValid());
    }

    @Test
    @DisplayName("AST validator strictly blocks all mutation statements (DELETE, UPDATE, DROP, INSERT, TRUNCATE)")
    void testMutationsBlocked() {
        assertFalse(sqlValidator.validateAndSanitize("DELETE FROM candidates WHERE current_stage = 'REJECTED'").isValid());
        assertFalse(sqlValidator.validateAndSanitize("UPDATE candidates SET current_stage = 'HIRED' WHERE id = 1").isValid());
        assertFalse(sqlValidator.validateAndSanitize("DROP TABLE candidates").isValid());
        assertFalse(sqlValidator.validateAndSanitize("TRUNCATE candidates").isValid());
        assertFalse(sqlValidator.validateAndSanitize("INSERT INTO candidates (name, email) VALUES ('Hacker', 'hacker@example.com')").isValid());
    }

    @Test
    @DisplayName("AST validator blocks multi-statements, non-whitelisted tables, and dangerous PostgreSQL functions")
    void testSecurityDefenses() {
        assertFalse(sqlValidator.validateAndSanitize("SELECT * FROM candidates; DROP TABLE candidates;").isValid(), "Multi-statements must fail");
        assertFalse(sqlValidator.validateAndSanitize("SELECT pg_sleep(5)").isValid(), "pg_sleep must fail");
        assertFalse(sqlValidator.validateAndSanitize("SELECT * FROM pg_shadow").isValid(), "System catalogs must fail");
        assertFalse(sqlValidator.validateAndSanitize("SELECT * FROM user_passwords").isValid(), "Non-whitelisted tables must fail");
    }
}
