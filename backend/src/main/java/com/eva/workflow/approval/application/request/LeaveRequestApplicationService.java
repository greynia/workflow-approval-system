package com.eva.workflow.approval.application.request;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.api.dto.common.PageResponse;
import com.eva.workflow.approval.api.dto.leave.ApprovalActionResponse;
import com.eva.workflow.approval.api.dto.leave.ApprovalStepResponse;
import com.eva.workflow.approval.api.dto.leave.CreateLeaveRequest;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestDetailResponse;
import com.eva.workflow.approval.api.dto.leave.LeaveRequestSummaryResponse;
import com.eva.workflow.approval.api.exception.BadRequestException;
import com.eva.workflow.approval.api.exception.ResourceNotFoundException;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.common.enums.RequestStatus;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.LeaveRequestEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalActionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.ApprovalStepRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.AuditLogRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.LeaveRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveRequestApplicationService {

    private static final String LEAVE_REQUEST_ENTITY_TYPE = "LEAVE_REQUEST";
    private static final String CREATE_LEAVE_REQUEST_ACTION = "CREATE";

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogRepository auditLogRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public LeaveRequestDetailResponse createRequest(AuthenticatedEmployee authenticatedEmployee, CreateLeaveRequest request) {
        validateCreateRequest(request);

        EmployeeEntity applicant = findActiveEmployee(authenticatedEmployee.employeeId(), "Applicant not found");
        EmployeeEntity deputy = resolveDeputy(request.deputyId(), applicant.getId());

        LeaveRequestEntity saved = leaveRequestRepository.save(LeaveRequestEntity.create(
                applicant,
                deputy,
                request.type(),
                request.startDate(),
                request.endDate(),
                request.days(),
                request.reason(),
                RequestStatus.PENDING
        ));

        auditLogRepository.save(com.eva.workflow.approval.infrastructure.persistence.jpa.entity.AuditLogEntity.create(
                LEAVE_REQUEST_ENTITY_TYPE,
                saved.getId(),
                CREATE_LEAVE_REQUEST_ACTION,
                applicant,
                buildCreateAuditDetail(saved)
        ));

        return leaveRequestMapper.toDetailResponse(saved, List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestSummaryResponse> getRequests(
            AuthenticatedEmployee authenticatedEmployee,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveRequestEntity> result = leaveRequestRepository.findByApplicantIdOrderByCreatedAtDesc(
                authenticatedEmployee.employeeId(),
                pageable
        );

        return new PageResponse<>(
                result.getContent().stream().map(leaveRequestMapper::toSummaryResponse).toList(),
                result.getNumber(),
                result.getTotalElements(),
                result.getSize(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public LeaveRequestDetailResponse getRequestDetail(AuthenticatedEmployee authenticatedEmployee, Long requestId) {
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(requestId)
                .filter(entity -> entity.getApplicant().getId().equals(authenticatedEmployee.employeeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        List<ApprovalStepResponse> steps = approvalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(requestId).stream()
                .map(leaveRequestMapper::toStepResponse)
                .toList();
        List<ApprovalActionResponse> actions = approvalActionRepository
                .findByApprovalStepLeaveRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(leaveRequestMapper::toActionResponse)
                .toList();

        return leaveRequestMapper.toDetailResponse(leaveRequest, steps, actions);
    }

    private void validateCreateRequest(CreateLeaveRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("endDate must be on or after startDate");
        }
    }

    private EmployeeEntity findActiveEmployee(Long employeeId, String notFoundMessage) {
        return employeeRepository.findById(employeeId)
                .filter(EmployeeEntity::getActive)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private EmployeeEntity resolveDeputy(Long deputyId, Long applicantId) {
        if (deputyId == null) {
            return null;
        }
        if (deputyId.equals(applicantId)) {
            throw new BadRequestException("deputyId cannot be the same as applicant");
        }
        return findActiveEmployee(deputyId, "Deputy not found");
    }

    private String buildCreateAuditDetail(LeaveRequestEntity entity) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", entity.getStatus(),
                    "type", entity.getType(),
                    "days", entity.getDays()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit detail", exception);
        }
    }
}
