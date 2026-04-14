package com.eva.workflow.approval.domain.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.api.exception.BusinessRuleException;
import com.eva.workflow.approval.common.enums.ApproverType;
import com.eva.workflow.approval.domain.approval.model.ApprovalFlowStep;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowDefinitionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowRuleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowDefinitionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApprovalFlowEngineTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowRuleRepository workflowRuleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private ApprovalFlowEngine approvalFlowEngine;

    @BeforeEach
    void setUp() {
        approvalFlowEngine = new ApprovalFlowEngine(
                workflowDefinitionRepository,
                workflowRuleRepository,
                employeeRepository,
                new ObjectMapper()
        );
    }

    @Test
    void generateStepsReturnsDirectManagerWhenDaysIsThree() {
        EmployeeEntity ceo = employee(1L, null, true);
        EmployeeEntity manager = employee(5L, 1L, true);
        EmployeeEntity applicant = employee(7L, 5L, true);
        mockWorkflow(rule(1L, 1, "{\"maxDays\":3}", ApproverType.DIRECT_MANAGER));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(manager));

        List<ApprovalFlowStep> steps = approvalFlowEngine.generateSteps(applicant, 3);

        assertThat(steps).containsExactly(new ApprovalFlowStep(1, 5L));
    }

    @Test
    void generateStepsReturnsTwoUniqueApproversWhenDaysExceedsThree() {
        EmployeeEntity ceo = employee(1L, null, true);
        EmployeeEntity manager = employee(5L, 1L, true);
        EmployeeEntity applicant = employee(7L, 5L, true);
        mockWorkflow(
                rule(2L, 2, "{\"minDays\":4}", ApproverType.DIRECT_MANAGER),
                rule(3L, 3, "{\"minDays\":4}", ApproverType.DEPARTMENT_MANAGER)
        );
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(ceo));

        List<ApprovalFlowStep> steps = approvalFlowEngine.generateSteps(applicant, 4);

        assertThat(steps).containsExactly(
                new ApprovalFlowStep(1, 5L),
                new ApprovalFlowStep(2, 1L)
        );
    }

    @Test
    void generateStepsSkipsSelfApproverAndEscalatesUpward() {
        EmployeeEntity ceo = employee(1L, null, true);
        EmployeeEntity managerApplicant = employee(5L, 1L, true);
        mockWorkflow(rule(1L, 1, "{\"maxDays\":3}", ApproverType.DIRECT_MANAGER));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(ceo));

        List<ApprovalFlowStep> steps = approvalFlowEngine.generateSteps(managerApplicant, 2);

        assertThat(steps).containsExactly(new ApprovalFlowStep(1, 1L));
    }

    @Test
    void generateStepsFailsWhenNoDistinctDepartmentManagerExists() {
        EmployeeEntity manager = employee(5L, null, true);
        EmployeeEntity applicant = employee(7L, 5L, true);
        mockWorkflow(
                rule(2L, 2, "{\"minDays\":4}", ApproverType.DIRECT_MANAGER),
                rule(3L, 3, "{\"minDays\":4}", ApproverType.DEPARTMENT_MANAGER)
        );
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> approvalFlowEngine.generateSteps(applicant, 5))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No valid approver");
    }

    @Test
    void generateStepsFailsWhenNoRuleMatches() {
        EmployeeEntity applicant = employee(7L, 5L, true);
        mockWorkflow(rule(1L, 1, "{\"maxDays\":3}", ApproverType.DIRECT_MANAGER));

        assertThatThrownBy(() -> approvalFlowEngine.generateSteps(applicant, 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No workflow rule matched");
    }

    private void mockWorkflow(WorkflowRuleEntity... rules) {
        WorkflowDefinitionEntity workflowDefinition = newInstance(WorkflowDefinitionEntity.class);
        setId(workflowDefinition, 1L);
        when(workflowDefinitionRepository.findFirstByActiveTrueOrderByIdAsc())
                .thenReturn(Optional.of(workflowDefinition));
        when(workflowRuleRepository.findByWorkflowDefinitionIdOrderByPriorityAsc(1L))
                .thenReturn(List.of(rules));
    }

    private WorkflowRuleEntity rule(Long id, int priority, String conditionJson, ApproverType approverType) {
        WorkflowRuleEntity entity = newInstance(WorkflowRuleEntity.class);
        setId(entity, id);
        setInt(entity, "priority", priority);
        setString(entity, "conditionJson", conditionJson);
        setEnum(entity, "approverType", approverType);
        return entity;
    }

    private EmployeeEntity employee(Long id, Long managerId, boolean active) {
        EmployeeEntity entity = newInstance(EmployeeEntity.class);
        setId(entity, id);
        setBoolean(entity, "active", active);
        if (managerId != null) {
            EmployeeEntity manager = newInstance(EmployeeEntity.class);
            setId(manager, managerId);
            setField(entity, "manager", manager);
        }
        return entity;
    }

    private void setId(Object target, Long value) {
        setField(target, "id", value);
    }

    private void setInt(Object target, String fieldName, int value) {
        setField(target, fieldName, Integer.valueOf(value));
    }

    private void setString(Object target, String fieldName, String value) {
        setField(target, fieldName, value);
    }

    private void setBoolean(Object target, String fieldName, boolean value) {
        setField(target, fieldName, Boolean.valueOf(value));
    }

    private void setEnum(Object target, String fieldName, Enum<?> value) {
        setField(target, fieldName, value);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T newInstance(Class<T> type) {
        try {
            java.lang.reflect.Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
