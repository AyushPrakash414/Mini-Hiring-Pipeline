package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SqlSecurityValidatorService {

    private static final Logger log = LoggerFactory.getLogger(SqlSecurityValidatorService.class);

    private static final Set<String> ALLOWED_TABLES = Set.of(
        "candidates",
        "candidate_stage_history"
    );

    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        "\\b(pg_sleep|pg_read_file|pg_ls_dir|dblink|copy|into\\s+outfile|into\\s+dumpfile|load_file|current_user|session_user|pg_shadow|pg_authid|information_schema|pg_catalog)\\b",
        Pattern.CASE_INSENSITIVE
    );

    public record ValidationResult(boolean isValid, String sanitizedSql, String errorMessage) {}

    /**
     * Parse and validate SQL statement against strict Read-Only and whitelist constraints.
     */
    public ValidationResult validateAndSanitize(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            return new ValidationResult(false, null, "SQL query cannot be empty.");
        }

        String sql = rawSql.trim();

        // Remove trailing semicolons and clean markdown backticks if any
        if (sql.startsWith("```sql")) {
            sql = sql.substring(6);
        } else if (sql.startsWith("```")) {
            sql = sql.substring(3);
        }
        if (sql.endsWith("```")) {
            sql = sql.substring(0, sql.length() - 3);
        }
        sql = sql.trim();
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        // 1. Check for dangerous functions / system catalog access
        if (DANGEROUS_PATTERNS.matcher(sql).find()) {
            return new ValidationResult(false, null, "Forbidden function or system catalog reference detected.");
        }

        try {
            // 2. Multi-statement check (prevent chained queries with ;)
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            if (statements.getStatements().size() != 1) {
                return new ValidationResult(false, null, "Multiple SQL statements are strictly forbidden.");
            }

            Statement statement = statements.getStatements().get(0);

            // 3. Strict SELECT verification
            if (!(statement instanceof Select selectStatement)) {
                return new ValidationResult(false, null, "Only SELECT queries are allowed. Mutation statements are blocked.");
            }

            // 4. Table Whitelist Verification
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(statement);

            for (String table : tableList) {
                String cleanTable = table.replace("\"", "").toLowerCase(Locale.ROOT);
                if (!ALLOWED_TABLES.contains(cleanTable)) {
                    return new ValidationResult(false, null, "Access to table '" + table + "' is not permitted. Only 'candidates' and 'candidate_stage_history' are allowed.");
                }
            }

            // 5. Enforce LIMIT <= 50
            if (selectStatement.getSelectBody() instanceof PlainSelect plainSelect) {
                Limit limit = plainSelect.getLimit();
                if (limit == null) {
                    limit = new Limit();
                    limit.setRowCount(new LongValue(50));
                    plainSelect.setLimit(limit);
                } else if (limit.getRowCount() instanceof LongValue longVal) {
                    if (longVal.getValue() > 50 || longVal.getValue() <= 0) {
                        limit.setRowCount(new LongValue(50));
                    }
                }
            }

            String validatedSql = selectStatement.toString();
            return new ValidationResult(true, validatedSql, null);

        } catch (JSQLParserException e) {
            log.warn("SQL Parsing failed for query: {}", sql, e);
            return new ValidationResult(false, null, "Invalid SQL syntax: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during SQL validation", e);
            return new ValidationResult(false, null, "Security validation error: " + e.getMessage());
        }
    }
}
