package com.eva.workflow.approval.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.eva.workflow.approval.common.enums.LeaveType;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;

class AiReviewPromptBuilderHashTest {

    private static final ReviewSnapshot SNAPSHOT = new ReviewSnapshot(
            1L,
            LeaveType.ANNUAL,
            LocalDateTime.of(2026, 5, 1, 9, 0),
            LocalDateTime.of(2026, 5, 1, 17, 0),
            480,
            "family trip",
            Instant.EPOCH,
            2L,
            400L,
            UserRole.EMPLOYEE,
            "Engineering",
            0,
            1
    );

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
    void render_substitutes_all_placeholders() {
        String prompt = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_EN, SNAPSHOT, List.of(), "en", "");

        assertThat(prompt).doesNotContain("{{");
        assertThat(prompt).contains("- Type: ANNUAL");
        assertThat(prompt).contains("- Duration: 1 workday");
        assertThat(prompt).contains("family trip");
        assertThat(prompt).contains("- Department: Engineering");
    }

    @Test
    void render_zh_uses_localized_data_formatting() {
        String prompt = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_ZH, SNAPSHOT, List.of(), "zh", "");

        assertThat(prompt).doesNotContain("{{");
        assertThat(prompt).contains("Traditional Chinese");
        assertThat(prompt).contains("1 個工作天");
    }

    @Test
    void render_treats_braces_in_user_reason_as_literal_text() {
        ReviewSnapshot withBraces = new ReviewSnapshot(
                1L, LeaveType.PERSONAL,
                LocalDateTime.of(2026, 5, 1, 9, 0), LocalDateTime.of(2026, 5, 1, 17, 0),
                480, "see {{department}} note and {{unknownToken}}", Instant.EPOCH,
                2L, 400L, UserRole.EMPLOYEE, "Engineering", 0, 1);

        String prompt = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_EN, withBraces, List.of(), "en", "");

        // The reason's braces are left verbatim and not mistaken for placeholders.
        assertThat(prompt).contains("see {{department}} note and {{unknownToken}}");
        // The real department placeholder is still substituted, not overwritten by the reason.
        assertThat(prompt).contains("- Department: Engineering");
    }

    @Test
    void render_rejects_unknown_placeholder_in_template() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> AiReviewPromptBuilder.render(
                        "prefix {{bogus}} suffix", SNAPSHOT, List.of(), "en", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void render_includes_risk_flags_block_when_present() {
        HardRuleFlag flag = new HardRuleFlag(
                "NEW_HIRE_SHORT_LEAVE", RiskLevel.MEDIUM, "New hire requesting leave within 90 days");
        String withFlags = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_EN, SNAPSHOT, List.of(flag), "en", "");
        String withoutFlags = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_EN, SNAPSHOT, List.of(), "en", "");

        assertThat(withFlags).contains("Risk flags detected:");
        assertThat(withFlags).contains("[MEDIUM] New hire requesting leave within 90 days");
        assertThat(withoutFlags).doesNotContain("Risk flags detected:");
    }

    @Test
    void render_includes_policy_context_when_present_and_omits_when_blank() {
        String withPolicy = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_EN, SNAPSHOT, List.of(), "en",
                "Company Policy Excerpts:\n- [Leave Limit] new hires need approval\n");
        String withoutPolicy = AiReviewPromptBuilder.render(
                AiReviewPromptBuilder.DEFAULT_TEMPLATE_EN, SNAPSHOT, List.of(), "en", "");

        assertThat(withPolicy).contains("Company Policy Excerpts:");
        assertThat(withPolicy).contains("[Leave Limit] new hires need approval");
        assertThat(withoutPolicy).doesNotContain("Company Policy Excerpts:");
        assertThat(withoutPolicy).doesNotContain("{{policyContext}}");
    }
}
