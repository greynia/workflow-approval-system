package com.eva.workflow.approval.domain.aireview.service;

import java.util.List;

import com.eva.workflow.approval.domain.aireview.model.AiReviewResult;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;

public interface AiReviewPort {
    AiReviewResult review(ReviewSnapshot snapshot, List<HardRuleFlag> flags, String locale);
}
