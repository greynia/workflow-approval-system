package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.Instant;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A chunk of company policy text plus its embedding, stored in a pgvector column.
 * Immutable reference data loaded by ingestion; never edited in place.
 */
@Getter
@Entity
@Table(name = "policy_chunks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyChunkEntity {

    public static final int DIMENSION = 768;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, length = 200)
    private String section;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = DIMENSION)
    @Column(nullable = false, columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "embedding_model", nullable = false, length = 60)
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static PolicyChunkEntity create(
            String locale,
            String section,
            String content,
            String contentHash,
            float[] embedding,
            String embeddingModel
    ) {
        PolicyChunkEntity entity = new PolicyChunkEntity();
        entity.locale = locale;
        entity.section = section;
        entity.content = content;
        entity.contentHash = contentHash;
        entity.embedding = embedding;
        entity.embeddingModel = embeddingModel;
        entity.createdAt = Instant.now();
        return entity;
    }
}
