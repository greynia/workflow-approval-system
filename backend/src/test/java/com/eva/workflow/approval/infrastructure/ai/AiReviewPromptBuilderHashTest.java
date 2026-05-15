package com.eva.workflow.approval.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiReviewPromptBuilderHashTest {

    @Test
    void promptVersionHash_is_8_char_hex() {
        String hash = AiReviewPromptBuilder.PROMPT_VERSION_HASH;
        assertThat(hash).hasSize(8);
        assertThat(hash).matches("[0-9a-f]{8}");
    }

    @Test
    void promptVersionHash_is_stable_across_reads() {
        String first = AiReviewPromptBuilder.PROMPT_VERSION_HASH;
        String second = AiReviewPromptBuilder.PROMPT_VERSION_HASH;
        assertThat(first).isEqualTo(second);
    }

    @Test
    void promptVersionHash_is_non_empty() {
        assertThat(AiReviewPromptBuilder.PROMPT_VERSION_HASH).isNotBlank();
    }
}
