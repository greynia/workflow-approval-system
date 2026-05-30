package com.eva.workflow.approval.infrastructure.ai.jina;

import java.util.List;

import com.eva.workflow.approval.domain.policy.service.RerankPort;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * {@link RerankPort} backed by a LangChain4j {@link ScoringModel} (Jina cross-encoder).
 * Holds no Spring wiring so it is trivially unit-testable with a stub scoring model;
 * {@link JinaRerankConfiguration} builds and registers it only when a Jina key is set.
 */
public class JinaRerankAdapter implements RerankPort {

    private final ScoringModel scoringModel;

    public JinaRerankAdapter(ScoringModel scoringModel) {
        this.scoringModel = scoringModel;
    }

    @Override
    public List<Double> scoreAll(String query, List<String> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        List<TextSegment> segments = documents.stream().map(TextSegment::from).toList();
        return scoringModel.scoreAll(segments, query).content();
    }
}
