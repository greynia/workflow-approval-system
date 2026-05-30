package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PolicyChunkEntity;

public interface PolicyChunkRepository extends JpaRepository<PolicyChunkEntity, Long> {

    @Modifying
    @Query("DELETE FROM PolicyChunkEntity p WHERE p.locale = :locale")
    int deleteByLocale(@Param("locale") String locale);

    /**
     * Top-{@code topK} chunks ordered by cosine distance to the query vector.
     * {@code vec} is the embedding as a pgvector literal, e.g. {@code [0.1,0.2,...]}.
     * A null {@code locale} searches across all locales.
     */
    @Query(value = """
            SELECT section AS section,
                   source AS source,
                   chunk_index AS chunkIndex,
                   content AS content,
                   1 - (embedding <=> CAST(:vec AS vector)) AS score
            FROM policy_chunks
            WHERE (:locale IS NULL OR locale = :locale)
            ORDER BY embedding <=> CAST(:vec AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<PolicyChunkMatchRow> searchSimilar(
            @Param("vec") String vec,
            @Param("locale") String locale,
            @Param("topK") int topK);
}
