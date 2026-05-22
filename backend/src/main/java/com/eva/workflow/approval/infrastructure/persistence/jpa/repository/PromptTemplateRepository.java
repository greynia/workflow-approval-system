package com.eva.workflow.approval.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.PromptTemplateStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PromptTemplateEntity;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplateEntity, Long> {

    /** Model-specific ACTIVE override (most specific match). */
    Optional<PromptTemplateEntity> findFirstByNameAndLocaleAndProviderAndStatus(
            String name, String locale, AiProvider provider, PromptTemplateStatus status);

    /** Shared ACTIVE template (no model override). */
    Optional<PromptTemplateEntity> findFirstByNameAndLocaleAndProviderIsNullAndStatus(
            String name, String locale, PromptTemplateStatus status);

    /** First ACTIVE row for a name (used to report the "current" version in admin stats). */
    Optional<PromptTemplateEntity> findFirstByNameAndStatusOrderByLocaleAsc(
            String name, PromptTemplateStatus status);

    List<PromptTemplateEntity> findAllByOrderByNameAscLocaleAscIdDesc(Pageable pageable);
}
