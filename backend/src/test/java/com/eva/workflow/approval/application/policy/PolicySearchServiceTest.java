package com.eva.workflow.approval.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.api.dto.aireview.PolicySearchResponse;
import com.eva.workflow.approval.application.audit.AuditLogService;
import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.application.exception.BadRequestApplicationException;
import com.eva.workflow.approval.application.exception.ForbiddenApplicationException;
import com.eva.workflow.approval.common.enums.UserRole;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class PolicySearchServiceTest {

    @Mock
    private PolicyRetrievalService policyRetrievalService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Test
    void mapsRetrievalToResponseAndAuditsAgainstActor() {
        PolicySearchService service =
                new PolicySearchService(policyRetrievalService, auditLogService, employeeRepository);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mock(EmployeeEntity.class)));
        when(policyRetrievalService.retrieve("請假", "zh", null)).thenReturn(new PolicyRetrieval(
                true, 5, 3,
                List.of(new PolicyMatch("A", "leave-policy.zh.md", 2, "A content", 0.8))));

        PolicySearchResponse response = service.search(admin(), "請假", null, "zh");

        assertThat(response.reranked()).isTrue();
        assertThat(response.topK()).isEqualTo(5);
        assertThat(response.matches()).singleElement().satisfies(match -> {
            assertThat(match.section()).isEqualTo("A");
            assertThat(match.source()).isEqualTo("leave-policy.zh.md");
            assertThat(match.chunkIndex()).isEqualTo(2);
            assertThat(match.score()).isEqualTo(0.8);
        });
        verify(auditLogService).log(
                eq("AI_POLICY_SEARCH"),
                eq(1L),
                eq("READ"),
                any(EmployeeEntity.class),
                eq(Map.of("candidateCount", 3, "matchCount", 1, "reranked", true)));
    }

    @Test
    void nonPrivilegedCallerIsForbiddenAndDoesNotRetrieve() {
        PolicySearchService service =
                new PolicySearchService(policyRetrievalService, auditLogService, employeeRepository);

        assertThatThrownBy(() -> service.search(employee(), "請假", null, "zh"))
                .isInstanceOf(ForbiddenApplicationException.class);
        verifyNoInteractions(policyRetrievalService);
    }

    @Test
    void blankQueryIsRejected() {
        PolicySearchService service =
                new PolicySearchService(policyRetrievalService, auditLogService, employeeRepository);

        assertThatThrownBy(() -> service.search(admin(), "  ", null, "zh"))
                .isInstanceOf(BadRequestApplicationException.class);
        verifyNoInteractions(policyRetrievalService);
    }

    private AuthenticatedEmployee admin() {
        return new AuthenticatedEmployee(1L, "admin@example.com", "Admin", UserRole.ADMIN, List.of());
    }

    private AuthenticatedEmployee employee() {
        return new AuthenticatedEmployee(2L, "user@example.com", "User", UserRole.EMPLOYEE, List.of());
    }
}
