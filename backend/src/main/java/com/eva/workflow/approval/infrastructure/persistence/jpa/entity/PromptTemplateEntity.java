package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.Instant;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A versioned, immutable prompt template. Selection key is {@code (name, locale)}
 * with an optional {@code provider} override; resolution is most-specific-wins
 * (a model-specific ACTIVE row beats the shared {@code provider IS NULL} row).
 */
@Getter
@Entity
@Table(name = "prompt_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 10)
    private String locale;

    /** {@code null} = shared across all models; otherwise a model-specific override. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AiProvider provider;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
    private String templateText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromptTemplateStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}
