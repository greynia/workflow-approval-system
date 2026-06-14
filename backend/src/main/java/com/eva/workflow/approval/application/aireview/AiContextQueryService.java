package com.eva.workflow.approval.application.aireview;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.aireview.AiContextResponse;
import com.eva.workflow.approval.api.dto.aireview.AiContextResponse.ApplicantInfo;
import com.eva.workflow.approval.api.dto.aireview.AiContextResponse.ApprovalStepInfo;
import com.eva.workflow.approval.api.dto.aireview.AiContextResponse.RequestInfo;
import com.eva.workflow.approval.api.dto.aireview.AiContextResponse.Stats;
import com.eva.workflow.approval.api.dto.aireview.HardRuleFlagResponse;
import com.eva.workflow.approval.api.dto.aireview.RuleEvaluationResponse;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.domain.aireview.model.HardRuleFlag;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.domain.aireview.service.AiHardRuleEngine;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.ApprovalStepEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiContextQueryService {

    private final ReviewSnapshotAssembler snapshotAssembler;
    private final ApprovalStepRepository approvalStepRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;
    private final AiHardRuleEngine ruleEngine;
    private final Clock clock;

    @Transactional
    public AiContextResponse getContext(AuthenticatedEmployee actor, Long requestId) {
        requireAiAgentOrAdmin(actor);

        ReviewSnapshot snapshot = snapshotAssembler.assemble(requestId);
        List<ApprovalStepEntity> steps =
                approvalStepRepository.findByLeaveRequestIdWithApproverOrderByStepOrderAsc(requestId);

        AiContextResponse response = new AiContextResponse(
                new RequestInfo(
                        snapshot.requestId(),
                        snapshot.leaveType(),
                        snapshot.startTime(),
                        snapshot.endTime(),
                        snapshot.durationMinutes(),
                        snapshot.reason(),
                        snapshot.requestCreatedAt()
                ),
                new ApplicantInfo(
                        snapshot.applicantId(),
                        snapshot.applicantRole(),
                        snapshot.departmentName(),
                        snapshot.applicantTenureDays()
                ),
                new Stats(snapshot.recentLeaveCountLast30Days()),
                steps.stream()
                        .map(step -> new ApprovalStepInfo(
                                step.getStepOrder(),
                                step.getStepType(),
                                step.getStatus(),
                                step.getApprover().getRole()
                        ))
                        .toList()
        );

        auditLogService.log("AI_CONTEXT", requestId, "READ", resolveActorEntity(actor), null);
        return response;
    }

    @Transactional
    public RuleEvaluationResponse evaluateRules(AuthenticatedEmployee actor, Long requestId, String locale) {
        requireAiAgentOrAdmin(actor);

        ReviewSnapshot snapshot = snapshotAssembler.assemble(requestId);
        List<HardRuleFlag> flags = ruleEngine.evaluate(snapshot, locale);

        RuleEvaluationResponse response = new RuleEvaluationResponse(
                requestId,
                clock.instant(),
                ruleEngine.highestRiskLevel(flags),
                flags.stream()
                        .map(f -> new HardRuleFlagResponse(f.code(), f.level(), f.humanReadable()))
                        .toList()
        );

        auditLogService.log(
                "AI_RULE_EVAL",
                requestId,
                "READ",
                resolveActorEntity(actor),
                Map.of("flagCount", flags.size())
        );
        return response;
    }

    private void requireAiAgentOrAdmin(AuthenticatedEmployee actor) {
        UserRole role = actor.role();
        if (role != UserRole.AI_AGENT && role != UserRole.ADMIN) {
            throw new ForbiddenApplicationException("Only AI agents or admins can read AI context");
        }
    }

    private EmployeeEntity resolveActorEntity(AuthenticatedEmployee actor) {
        return employeeRepository.findById(actor.employeeId())
                .orElseThrow(() -> new ResourceNotFoundApplicationException(
                        "Actor employee not found: " + actor.employeeId()));
    }
}
