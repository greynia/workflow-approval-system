package com.eva.workflow.approval.domain.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eva.workflow.approval.common.enums.ApprovalStepType;
import com.eva.workflow.approval.common.enums.ApproverType;
import com.eva.workflow.approval.domain.approval.exception.BusinessRuleException;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowContext;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep;
import com.eva.workflow.approval.domain.approval.model.ApprovalRule;

class ApprovalFlowEngineTest {

    private ApprovalFlowEngine approvalFlowEngine;

    @BeforeEach
    void setUp() {
        approvalFlowEngine = new ApprovalFlowEngine();
    }

    @Test
    void generateStepsStartsWithDeputyThenDirectManagerWhenDurationWithinThreshold() {
        ApprovalFlowContext context = new ApprovalFlowContext(
                7L,
                6L,
                1440,
                List.of(5L, 2L),
                List.of(rule(null, 1440, ApproverType.DIRECT_MANAGER, 1))
        );

        List<ApprovalFlowStep> steps = approvalFlowEngine.generateSteps(context);

        assertThat(steps).containsExactly(
                new ApprovalFlowStep(1, 6L, ApprovalStepType.DEPUTY),
                new ApprovalFlowStep(2, 5L, ApprovalStepType.MANAGER)
        );
    }

    @Test
    void generateStepsReturnsDeputyThenTwoUniqueManagersWhenDurationExceedsThreshold() {
        ApprovalFlowContext context = new ApprovalFlowContext(
                7L,
                6L,
                1920,
                List.of(5L, 2L),
                List.of(
                        rule(1441, null, ApproverType.DIRECT_MANAGER, 2),
                        rule(1441, null, ApproverType.DEPARTMENT_MANAGER, 3)
                )
        );

        List<ApprovalFlowStep> steps = approvalFlowEngine.generateSteps(context);

        assertThat(steps).containsExactly(
                new ApprovalFlowStep(1, 6L, ApprovalStepType.DEPUTY),
                new ApprovalFlowStep(2, 5L, ApprovalStepType.MANAGER),
                new ApprovalFlowStep(3, 2L, ApprovalStepType.MANAGER)
        );
    }

    @Test
    void generateStepsSkipsApplicantIdIfItAppearsInChain() {
        ApprovalFlowContext context = new ApprovalFlowContext(
                5L,
                6L,
                480,
                List.of(5L, 2L),
                List.of(rule(null, 1440, ApproverType.DIRECT_MANAGER, 1))
        );

        List<ApprovalFlowStep> steps = approvalFlowEngine.generateSteps(context);

        assertThat(steps).containsExactly(
                new ApprovalFlowStep(1, 6L, ApprovalStepType.DEPUTY),
                new ApprovalFlowStep(2, 2L, ApprovalStepType.MANAGER)
        );
    }

    @Test
    void generateStepsFailsWhenNoDistinctDepartmentManagerExists() {
        ApprovalFlowContext context = new ApprovalFlowContext(
                7L,
                6L,
                1920,
                List.of(5L),
                List.of(
                        rule(1441, null, ApproverType.DIRECT_MANAGER, 2),
                        rule(1441, null, ApproverType.DEPARTMENT_MANAGER, 3)
                )
        );

        assertThatThrownBy(() -> approvalFlowEngine.generateSteps(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No valid approver");
    }

    @Test
    void generateStepsFailsWhenNoRuleMatches() {
        ApprovalFlowContext context = new ApprovalFlowContext(
                7L,
                6L,
                4800,
                List.of(5L, 2L),
                List.of(rule(null, 1440, ApproverType.DIRECT_MANAGER, 1))
        );

        assertThatThrownBy(() -> approvalFlowEngine.generateSteps(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No workflow rule matched");
    }

    @Test
    void generateStepsFailsWhenDeputyIsMissing() {
        ApprovalFlowContext context = new ApprovalFlowContext(
                7L,
                null,
                480,
                List.of(5L, 2L),
                List.of(rule(null, 1440, ApproverType.DIRECT_MANAGER, 1))
        );

        assertThatThrownBy(() -> approvalFlowEngine.generateSteps(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No valid deputy selected");
    }

    private ApprovalRule rule(Integer minMinutes, Integer maxMinutes, ApproverType approverType, int stepOrder) {
        return new ApprovalRule(minMinutes, maxMinutes, approverType, stepOrder);
    }
}
