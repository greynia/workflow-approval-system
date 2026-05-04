package com.eva.workflow.approval.application.aireview;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.application.exception.ResourceNotFoundApplicationException;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.domain.aireview.model.ReviewSnapshot;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewSnapshotAssembler {

    private static final List<RequestStatus> ACTIVE_STATUSES = List.of(
            RequestStatus.PENDING, RequestStatus.APPROVED
    );

    private final LeaveRequestRepository leaveRequestRepository;
    private final ApprovalStepRepository approvalStepRepository;

    @Transactional(readOnly = true)
    public ReviewSnapshot assemble(Long requestId) {
        LeaveRequestEntity request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundApplicationException("Leave request not found: " + requestId));

        EmployeeEntity applicant = request.getApplicant();

        long tenureDays = ChronoUnit.DAYS.between(applicant.getHireDate(), LocalDate.now());

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        int recentCount = (int) leaveRequestRepository.countRecentByApplicantIdExcludingRequest(
                applicant.getId(), request.getId(), ACTIVE_STATUSES, thirtyDaysAgo);

        int stepCount = approvalStepRepository.findByLeaveRequestId(requestId).size();

        return new ReviewSnapshot(
                request.getId(),
                request.getType(),
                request.getStartTime(),
                request.getEndTime(),
                request.getDurationMinutes(),
                request.getReason(),
                request.getCreatedAt(),
                applicant.getId(),
                tenureDays,
                applicant.getRole(),
                applicant.getDepartment().getName(),
                recentCount,
                stepCount
        );
    }
}
