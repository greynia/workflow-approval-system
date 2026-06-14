package com.eva.workflow.approval.infrastructure.ai.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.eva.workflow.approval.domain.policy.service.EmbeddingTaskType;
import com.fasterxml.jackson.databind.ObjectMapper;

class GeminiEmbeddingAdapterTest {

    private GeminiEmbeddingAdapter adapter;

    @BeforeEach
    void setUp() {
        GeminiProperties props = new GeminiProperties(
                "test-api-key", "gemini-2.5-flash-lite", "text-embedding-004", 30);
        adapter = new GeminiEmbeddingAdapter(props, new ObjectMapper(), mock(WebClient.class));
    }

    @Test
    void parseEmbedding_reads_values_into_float_array() {
        String raw = "{\"embedding\":{\"values\":[0.1,0.2,0.3]}}";

        float[] vector = adapter.parseEmbedding(raw);

        assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void parseEmbedding_throws_when_values_missing() {
        assertThatThrownBy(() -> adapter.parseEmbedding("{}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI_EMBEDDING_PARSE_ERROR");
    }

    @Test
    void exposes_model_name_and_dimension() {
        assertThat(adapter.modelName()).isEqualTo("text-embedding-004");
        assertThat(adapter.dimension()).isEqualTo(768);
    }

    @Test
    void buildRequestBody_includes_taskType_when_present() {
        Map<String, Object> body = adapter.buildRequestBody("hello", EmbeddingTaskType.RETRIEVAL_DOCUMENT);

        assertThat(body).containsEntry("model", "models/text-embedding-004");
        assertThat(body).containsEntry("taskType", "RETRIEVAL_DOCUMENT");
    }

    @Test
    void buildRequestBody_omits_taskType_when_null() {
        Map<String, Object> body = adapter.buildRequestBody("hello", null);

        assertThat(body).doesNotContainKey("taskType");
    }
}
