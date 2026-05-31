package com.eva.workflow.approval.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.domain.policy.service.EmbeddingTaskType;
import com.eva.workflow.approval.domain.policy.service.RerankPort;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkMatchRow;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkRepository;

@ExtendWith(MockitoExtension.class)
class PolicyRetrievalServiceTest {

    @Mock
    private EmbeddingPort embeddingPort;

    @Mock
    private PolicyChunkRepository policyChunkRepository;

    private final PolicyProperties properties = new PolicyProperties(true, 600, 80, 25, 5, 0.5);

    @Test
    void withoutRerankerFallsBackToVectorOrderAndKeepsChunkCitation() {
        PolicyRetrievalService service = new PolicyRetrievalService(
                embeddingPort, Optional.empty(), policyChunkRepository, properties);
        when(embeddingPort.embed("請假", EmbeddingTaskType.RETRIEVAL_QUERY)).thenReturn(new float[]{1.0f, 0.0f});
        when(policyChunkRepository.searchSimilar("[1.0,0.0]", "zh", 25))
                .thenReturn(List.of(row("A", "leave-policy.zh.md", 2, "A content", 0.2)));

        PolicyRetrieval retrieval = service.retrieve("請假", "zh", null);

        assertThat(retrieval.reranked()).isFalse();
        assertThat(retrieval.finalTopK()).isEqualTo(5);
        assertThat(retrieval.candidateCount()).isEqualTo(1);
        assertThat(retrieval.matches()).singleElement().satisfies(match -> {
            assertThat(match.section()).isEqualTo("A");
            assertThat(match.source()).isEqualTo("leave-policy.zh.md");
            assertThat(match.chunkIndex()).isEqualTo(2);
            assertThat(match.score()).isEqualTo(0.2);
        });
    }

    @Test
    void withRerankerFiltersByMinScoreAndUsesRerankScoreNotCosine() {
        RerankPort rerankPort = mock(RerankPort.class);
        PolicyRetrievalService service = new PolicyRetrievalService(
                embeddingPort, Optional.of(rerankPort), policyChunkRepository, properties);
        List<PolicyChunkMatchRow> rows = List.of(
                row("Low", "leave-policy.zh.md", 0, "low", 0.99),
                row("High", "leave-policy.zh.md", 1, "high", 0.1));
        when(embeddingPort.embed("請假", EmbeddingTaskType.RETRIEVAL_QUERY)).thenReturn(new float[]{1.0f});
        when(policyChunkRepository.searchSimilar("[1.0]", null, 25)).thenReturn(rows);
        when(rerankPort.scoreAll("請假", List.of("Low\nlow", "High\nhigh"))).thenReturn(List.of(0.4, 0.8));

        PolicyRetrieval retrieval = service.retrieve("請假", null, 5);

        assertThat(retrieval.reranked()).isTrue();
        assertThat(retrieval.matches()).singleElement().satisfies(match -> {
            assertThat(match.section()).isEqualTo("High");
            assertThat(match.chunkIndex()).isEqualTo(1);
            assertThat(match.score()).isEqualTo(0.8);
        });
    }

    @Test
    void doesNotCallRerankerWhenVectorSearchReturnsNoCandidates() {
        RerankPort rerankPort = mock(RerankPort.class);
        PolicyRetrievalService service = new PolicyRetrievalService(
                embeddingPort, Optional.of(rerankPort), policyChunkRepository, properties);
        when(embeddingPort.embed("請假", EmbeddingTaskType.RETRIEVAL_QUERY)).thenReturn(new float[]{1.0f});
        when(policyChunkRepository.searchSimilar("[1.0]", null, 25)).thenReturn(List.of());

        PolicyRetrieval retrieval = service.retrieve("請假", null, null);

        assertThat(retrieval.reranked()).isFalse();
        assertThat(retrieval.matches()).isEmpty();
        verify(rerankPort, never()).scoreAll(any(), any());
    }

    private PolicyChunkMatchRow row(
            String section, String source, int chunkIndex, String content, double score) {
        return new PolicyChunkMatchRow() {
            @Override
            public String getSection() {
                return section;
            }

            @Override
            public String getSource() {
                return source;
            }

            @Override
            public int getChunkIndex() {
                return chunkIndex;
            }

            @Override
            public String getContent() {
                return content;
            }

            @Override
            public double getScore() {
                return score;
            }
        };
    }
}
