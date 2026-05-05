package com.eva.workflow.approval.infrastructure.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiReviewPort;

public class FallbackAiReviewAdapter implements AiReviewPort {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiReviewAdapter.class);

    private final List<AiReviewPort> chain;

    public FallbackAiReviewAdapter(List<AiReviewPort> chain) {
        this.chain = List.copyOf(chain);
    }

    @Override
    public AiReviewResult review(ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale) {
        RuntimeException lastException = null;
        for (AiReviewPort adapter : chain) {
            try {
                return adapter.review(snapshot, flags, locale);
            } catch (Exception e) {
                log.warn("AI adapter {} failed, trying next in chain: {}",
                        adapter.getClass().getSimpleName(), e.getMessage());
                lastException = (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        }
        throw lastException;
    }
}
