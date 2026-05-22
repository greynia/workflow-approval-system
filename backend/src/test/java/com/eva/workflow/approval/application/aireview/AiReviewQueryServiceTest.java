package com.eva.workflow.approval.application.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.api.dto.aireview.AiReviewResponse;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.request.LeaveRequestApplicationService;
import com.eva.workflow.approval.common.Permission;
import com.eva.workflow.approval.common.enums.AiProvider;
import com.eva.workflow.approval.common.enums.AiRecommendation;
import com.eva.workflow.approval.common.enums.AiReviewStatus;
import com.eva.workflow.approval.common.enums.RiskLevel;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AiReviewEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AiReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AiReviewQueryServiceTest {

    @Mock
    private AiReviewRepository aiReviewRepository;

    @Mock
    private LeaveRequestApplicationService leaveRequestApplicationService;

    private AiReviewQueryService aiReviewQueryService;

    @BeforeEach
    void setUp() {
        aiReviewQueryService = new AiReviewQueryService(
                aiReviewRepository,
                new ObjectMapper(),
                leaveRequestApplicationService
        );
    }

    @Test
    void getAiReviewChecksRequestAccessBeforeReturningReview() {
        AuthenticatedEmployee caller = managerWithApprovalView();
        AiReviewEntity pendingReview = AiReviewEntity.createPending(10L);
        when(aiReviewRepository.findByRequestId(10L)).thenReturn(Optional.of(pendingReview));

        AiReviewResponse response = aiReviewQueryService.getAiReview(caller, 10L);

        verify(leaveRequestApplicationService).assertCanAccessRequest(caller, 10L);
        assertThat(response.status()).isEqualTo(AiReviewStatus.PENDING);
    }

    @Test
    void getAiReviewReturnsHardRuleFlagsSeparately() {
        AuthenticatedEmployee caller = managerWithApprovalView();
        AiReviewEntity review = AiReviewEntity.createPending(10L);
        review.markCompleted(
                "AI summary",
                RiskLevel.MEDIUM,
                "[\"新進員工在到職 90 天內申請請假\"]",
                AiRecommendation.REVIEW_CAREFULLY,
                "Review carefully",
                "[{\"code\":\"NEW_HIRE_SHORT_LEAVE\",\"level\":\"MEDIUM\",\"humanReadable\":\"新進員工在到職 90 天內申請請假\"}]",
                "{}",
                "{\"summary\":\"AI summary\"}",
                "test-model",
                "v1",
                null,
                AiProvider.LOCAL,
                0,
                0,
                0,
                100,
                false,
                "[]"
        );
        when(aiReviewRepository.findByRequestId(10L)).thenReturn(Optional.of(review));

        AiReviewResponse response = aiReviewQueryService.getAiReview(caller, 10L);

        assertThat(response.hardRuleFlags()).hasSize(1);
        assertThat(response.hardRuleFlags().get(0).code()).isEqualTo("NEW_HIRE_SHORT_LEAVE");
        assertThat(response.hardRuleFlags().get(0).level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(response.hardRuleFlags().get(0).humanReadable()).isEqualTo("新進員工在到職 90 天內申請請假");
    }

    @Test
    void getAiReviewRejectsCallerWithoutApprovalViewBeforeRequestLookup() {
        AuthenticatedEmployee caller = new AuthenticatedEmployee(
                7L,
                "employee@example.com",
                "Employee",
                UserRole.EMPLOYEE,
                List.of(Permission.REQUEST_VIEW)
        );

        assertThatThrownBy(() -> aiReviewQueryService.getAiReview(caller, 10L))
                .isInstanceOf(ForbiddenApplicationException.class)
                .hasMessage("AI review access forbidden");

        verify(leaveRequestApplicationService, never()).assertCanAccessRequest(caller, 10L);
        verifyNoInteractions(aiReviewRepository);
    }

    private AuthenticatedEmployee managerWithApprovalView() {
        return new AuthenticatedEmployee(
                5L,
                "manager@example.com",
                "Manager",
                UserRole.MANAGER,
                List.of(Permission.APPROVAL_VIEW)
        );
    }
}
