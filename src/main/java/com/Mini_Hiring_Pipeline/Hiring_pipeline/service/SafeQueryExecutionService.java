package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SafeQueryExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SafeQueryExecutionService.class);

    private final JdbcTemplate jdbcTemplate;

    public SafeQueryExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes validated SQL query in strict read-only transaction mode and unwraps PostgreSQL custom types like citext.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeReadOnlyQuery(String validatedSql) {
        try {
            log.info("Executing safe read-only SQL: {}", validatedSql);
            List<Map<String, Object>> rawRows = jdbcTemplate.queryForList(validatedSql);

            List<Map<String, Object>> sanitizedRows = new ArrayList<>();
            for (Map<String, Object> row : rawRows) {
                Map<String, Object> cleanRow = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof PGobject pgObj) {
                        cleanRow.put(entry.getKey(), pgObj.getValue());
                    } else if (val != null && val.getClass().getName().contains("PGobject")) {
                        try {
                            cleanRow.put(entry.getKey(), val.getClass().getMethod("getValue").invoke(val));
                        } catch (Exception ignored) {
                            cleanRow.put(entry.getKey(), val.toString());
                        }
                    } else {
                        cleanRow.put(entry.getKey(), val);
                    }
                }
                sanitizedRows.add(cleanRow);
            }

            return sanitizedRows;
        } catch (Exception e) {
            log.error("Failed to execute read-only query: {}", validatedSql, e);
            throw new RuntimeException("Database query execution error: " + e.getMessage(), e);
        }
    }
}
