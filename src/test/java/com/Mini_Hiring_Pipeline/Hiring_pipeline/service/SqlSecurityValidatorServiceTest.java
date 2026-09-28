package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.service.SqlSecurityValidatorService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlSecurityValidatorServiceTest {

    private SqlSecurityValidatorService validator;

    @BeforeEach
    void setUp() {
        validator = new SqlSecurityValidatorService();
    }

    @Test
    void testValidSelectQueries() {
        ValidationResult res1 = validator.validateAndSanitize(
            "SELECT id, name, email, current_stage FROM candidates WHERE current_stage = 'INTERVIEW'"
        );
        assertTrue(res1.isValid(), "Valid SELECT on candidates should pass");
        assertTrue(res1.sanitizedSql().contains("LIMIT 50"), "Should auto-append LIMIT 50");

        ValidationResult res2 = validator.validateAndSanitize(
            "SELECT c.name, h.to_stage FROM candidates c JOIN candidate_stage_history h ON c.id = h.candidate_id LIMIT 10"
        );
        assertTrue(res2.isValid(), "Join between candidates and history should pass");
    }

    @Test
    void testBlockMutations() {
        ValidationResult deleteRes = validator.validateAndSanitize("DELETE FROM candidates WHERE current_stage = 'REJECTED'");
        assertFalse(deleteRes.isValid(), "DELETE must be blocked");

        ValidationResult updateRes = validator.validateAndSanitize("UPDATE candidates SET current_stage = 'HIRED' WHERE id = 1");
        assertFalse(updateRes.isValid(), "UPDATE must be blocked");

        ValidationResult dropRes = validator.validateAndSanitize("DROP TABLE candidates");
        assertFalse(dropRes.isValid(), "DROP TABLE must be blocked");

        ValidationResult insertRes = validator.validateAndSanitize("INSERT INTO candidates (name, email) VALUES ('Test', 'test@example.com')");
        assertFalse(insertRes.isValid(), "INSERT must be blocked");

        ValidationResult truncateRes = validator.validateAndSanitize("TRUNCATE candidates");
        assertFalse(truncateRes.isValid(), "TRUNCATE must be blocked");
    }

    @Test
    void testBlockMultiStatementsAndInjections() {
        ValidationResult chainedRes = validator.validateAndSanitize(
            "SELECT * FROM candidates; DROP TABLE candidates;"
        );
        assertFalse(chainedRes.isValid(), "Chained multi-statements must be blocked");
    }

    @Test
    void testBlockDangerousFunctionsAndSystemCatalogs() {
        ValidationResult sleepRes = validator.validateAndSanitize("SELECT pg_sleep(10)");
        assertFalse(sleepRes.isValid(), "pg_sleep must be blocked");

        ValidationResult catalogRes = validator.validateAndSanitize("SELECT * FROM pg_shadow");
        assertFalse(catalogRes.isValid(), "Access to pg_shadow must be blocked");

        ValidationResult otherTableRes = validator.validateAndSanitize("SELECT * FROM users");
        assertFalse(otherTableRes.isValid(), "Access to non-whitelisted tables must be blocked");
    }
}
