package com.eva.workflow.approval.infrastructure.ai;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eva.workflow.approval.domain.aireview.model.AiReviewAttempt;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;

public class FallbackAiReviewAdapter implements AiReviewPort {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiReviewAdapter.class);
    private static final int ERROR_MESSAGE_MAX = 500;

    private final List<AiReviewPort> chain;

    public FallbackAiReviewAdapter(List<AiReviewPort> chain) {
        this.chain = List.copyOf(chain);
    }

    @Override
    public AiReviewResult review(ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale) {
        int cumulativeMs = 0;
        List<AiReviewAttempt> attempts = new ArrayList<>();
        RuntimeException lastException = null;

        for (AiReviewPort adapter : chain) {
            long start = System.currentTimeMillis();
            try {
                AiReviewResult leaf = adapter.review(snapshot, flags, locale);
                int ms = (int) (System.currentTimeMillis() - start);
                cumulativeMs += ms;
                attempts.add(new AiReviewAttempt(
                        leaf.provider(), leaf.modelName(), ms, true, null));
                return withProvenance(leaf, attempts, cumulativeMs);
            } catch (Exception e) {
                int ms = (int) (System.currentTimeMillis() - start);
                cumulativeMs += ms;
                attempts.add(new AiReviewAttempt(
                        adapter.provider(),
                        adapter.modelName(),
                        ms,
                        false,
                        truncate(e.getMessage(), ERROR_MESSAGE_MAX)));
                log.warn("AI adapter {} failed, trying next in chain: {}",
                        adapter.getClass().getSimpleName(), e.getMessage());
                lastException = (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        }
        throw lastException;
    }

    private static AiReviewResult withProvenance(
            AiReviewResult leaf, List<AiReviewAttempt> attempts, int cumulativeMs) {
        return new AiReviewResult(
                leaf.summary(),
                leaf.riskLevel(),
                leaf.riskReasons(),
                leaf.recommendation(),
                leaf.recommendationReason(),
                leaf.modelName(),
                leaf.promptVersion(),
                leaf.promptTemplateId(),
                leaf.provider(),
                leaf.inputTokens(),
                leaf.outputTokens(),
                leaf.tokenUsage(),
                cumulativeMs,
                attempts.size() > 1,
                List.copyOf(attempts)
        );
    }

    private static String truncate(String message, int max) {
        if (message == null) {
            return null;
        }
        return message.length() <= max ? message : message.substring(0, max);
    }
}
