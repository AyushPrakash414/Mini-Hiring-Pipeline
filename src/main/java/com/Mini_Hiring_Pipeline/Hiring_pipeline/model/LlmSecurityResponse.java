package com.Mini_Hiring_Pipeline.Hiring_pipeline.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmSecurityResponse {

    public enum SecurityStatus {
        APPROVED,
        DENIED_MUTATION,
        DENIED_OUT_OF_CONTEXT,
        DENIED_MALICIOUS,
        VALIDATION_FAILED,
        API_KEY_MISSING,
        ERROR
    }

    @JsonProperty("is_relevant")
    @com.fasterxml.jackson.annotation.JsonAlias({"isRelevant", "relevant"})
    private boolean isRelevant;

    @JsonProperty("is_read_only")
    @com.fasterxml.jackson.annotation.JsonAlias({"isReadOnly", "readOnly", "read_only"})
    private boolean isReadOnly;

    @JsonProperty("is_safe")
    @com.fasterxml.jackson.annotation.JsonAlias({"isSafe", "safe"})
    private boolean isSafe;

    @JsonProperty("status")
    private SecurityStatus status;

    @JsonProperty("rejection_reason")
    @com.fasterxml.jackson.annotation.JsonAlias({"rejectionReason", "reason", "rejection"})
    private String rejectionReason;

    @JsonProperty("explanation")
    @com.fasterxml.jackson.annotation.JsonAlias({"description", "summary"})
    private String explanation;

    @JsonProperty("generated_sql")
    @com.fasterxml.jackson.annotation.JsonAlias({"generatedSql", "sql", "sql_query", "query"})
    private String generatedSql;

    @JsonProperty("original_query")
    @com.fasterxml.jackson.annotation.JsonAlias({"originalQuery"})
    private String originalQuery;

    @JsonProperty("query_results")
    @com.fasterxml.jackson.annotation.JsonAlias({"queryResults", "records", "data"})
    private List<Map<String, Object>> queryResults;

    @JsonProperty("result_count")
    @com.fasterxml.jackson.annotation.JsonAlias({"resultCount", "count"})
    private int resultCount;

    public LlmSecurityResponse() {}

    public LlmSecurityResponse(SecurityStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.isRelevant = false;
        this.isReadOnly = false;
        this.isSafe = false;
    }

    // Getters and Setters
    public boolean isRelevant() { return isRelevant; }
    public void setRelevant(boolean relevant) { isRelevant = relevant; }

    public boolean isReadOnly() { return isReadOnly; }
    public void setReadOnly(boolean readOnly) { isReadOnly = readOnly; }

    public boolean isSafe() { return isSafe; }
    public void setSafe(boolean safe) { isSafe = safe; }

    public SecurityStatus getStatus() { return status; }
    public void setStatus(SecurityStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getGeneratedSql() { return generatedSql; }
    public void setGeneratedSql(String generatedSql) { this.generatedSql = generatedSql; }

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public List<Map<String, Object>> getQueryResults() { return queryResults; }
    public void setQueryResults(List<Map<String, Object>> queryResults) { 
        this.queryResults = queryResults;
        this.resultCount = queryResults != null ? queryResults.size() : 0;
    }

    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }
}
