package com.eva.workflow.approval.domain.aireview.service;

import java.util.List;

import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;

public interface AiReviewPort {
    AiReviewResult review(ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale);

    default AiProvider provider() {
        return null;
    }

    default String modelName() {
        return null;
    }
}
