package com.Mini_Hiring_Pipeline.Hiring_pipeline.repository;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.Candidate;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.CandidateStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByEmail(String email);

    List<Candidate> findByCurrentStage(CandidateStage currentStage);

    /**
     * Typo-tolerant fuzzy candidate search using PostgreSQL pg_trgm.
     * Combines whole-string similarity and word-level similarity so single-word typos
     * (e.g. 'ayufh' -> 'Ayush', 'sharam' -> 'Sharma') match full candidate names accurately.
     */
    @Query(value = """
        SELECT 
            c.id AS id,
            c.name AS name,
            c.email::text AS email,
            c.phone AS phone,
            c.current_stage::text AS currentStage,
            c.created_at AS createdAt,
            ROUND(GREATEST(
                similarity(lower(c.name), lower(:query)),
                word_similarity(lower(:query), lower(c.name))
            )::numeric, 3) AS similarityScore
        FROM candidates c
        WHERE 
            word_similarity(lower(:query), lower(c.name)) >= 0.30
            OR similarity(lower(c.name), lower(:query)) >= 0.20
            OR lower(c.name) ILIKE CONCAT('%', lower(:query), '%')
        ORDER BY similarityScore DESC, c.name ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<CandidateSearchResult> searchByNameFuzzy(
        @Param("query") String query, 
        @Param("limit") int limit
    );
}
