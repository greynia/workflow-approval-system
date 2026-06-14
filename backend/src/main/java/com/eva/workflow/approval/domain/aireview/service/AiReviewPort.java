package com.eva.workflow.approval.domain.aireview.service;

import java.util.List;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;

public interface AiReviewPort {

    /**
     * @param policyContext pre-formatted company-policy excerpts to ground the review, or empty/blank
     *                      when none were retrieved (the prompt then omits the policy section)
     */
    AiReviewResult review(
            ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale, String policyContext);

    default AiProvider provider() {
        return null;
    }

    default String modelName() {
        return null;
    }
}
