package com.eva.workflow.approval.infrastructure.ai.jina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

class JinaRerankAdapterTest {

    @Test
    void scoreAll_delegates_to_scoring_model_preserving_order() {
        ScoringModel scoringModel = mock(ScoringModel.class);
        when(scoringModel.scoreAll(anyList(), eq("q")))
                .thenReturn(Response.from(List.of(0.9, 0.2)));
        JinaRerankAdapter adapter = new JinaRerankAdapter(scoringModel);

        List<Double> scores = adapter.scoreAll("q", List.of("doc-a", "doc-b"));

        assertThat(scores).containsExactly(0.9, 0.2);
    }

    @Test
    void scoreAll_returns_empty_without_calling_model_for_no_documents() {
        ScoringModel scoringModel = mock(ScoringModel.class);
        JinaRerankAdapter adapter = new JinaRerankAdapter(scoringModel);

        assertThat(adapter.scoreAll("q", List.of())).isEmpty();
        verify(scoringModel, never()).scoreAll(anyList(), eq("q"));
    }
}
