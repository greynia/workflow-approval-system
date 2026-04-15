package com.eva.workflow.approval.application.approval;

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

import com.eva.workflow.approval.application.exception.ApplicationConfigurationException;
import com.eva.workflow.approval.common.enums.ApproverType;
import com.eva.workflow.approval.domain.approval.model.ApprovalRule;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowDefinitionEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.WorkflowRuleEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowDefinitionRepository;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.WorkflowRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowRuleAssemblerTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowRuleRepository workflowRuleRepository;

    private WorkflowRuleAssembler workflowRuleAssembler;

    @BeforeEach
    void setUp() {
        workflowRuleAssembler = new WorkflowRuleAssembler(
                workflowDefinitionRepository,
                workflowRuleRepository,
                new ObjectMapper()
        );
    }

    @Test
    void loadActiveWorkflowRulesFailsWhenDefinitionMissing() {
        when(workflowDefinitionRepository.findFirstByActiveTrueOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowRuleAssembler.loadActiveWorkflowRules())
                .isInstanceOf(ApplicationConfigurationException.class)
                .hasMessageContaining("No active workflow definition found");
    }

    @Test
    void loadActiveWorkflowRulesMapsJsonIntoDomainRules() {
        WorkflowDefinitionEntity workflowDefinition = newInstance(WorkflowDefinitionEntity.class);
        setField(workflowDefinition, "id", 1L);

        WorkflowRuleEntity directRule = workflowRuleEntity(1L, 1, "{\"maxMinutes\":1440}", ApproverType.DIRECT_MANAGER);
        WorkflowRuleEntity departmentRule = workflowRuleEntity(2L, 2, "{\"minMinutes\":1920}", ApproverType.DEPARTMENT_MANAGER);

        when(workflowDefinitionRepository.findFirstByActiveTrueOrderByIdAsc())
                .thenReturn(Optional.of(workflowDefinition));
        when(workflowRuleRepository.findByWorkflowDefinitionIdOrderByPriorityAsc(1L))
                .thenReturn(List.of(directRule, departmentRule));

        List<ApprovalRule> rules = workflowRuleAssembler.loadActiveWorkflowRules();

        assertThat(rules).containsExactly(
                new ApprovalRule(null, 1440, ApproverType.DIRECT_MANAGER, 1),
                new ApprovalRule(1920, null, ApproverType.DEPARTMENT_MANAGER, 2)
        );
    }

    private WorkflowRuleEntity workflowRuleEntity(Long id, int priority, String conditionJson, ApproverType approverType) {
        WorkflowRuleEntity entity = newInstance(WorkflowRuleEntity.class);
        setField(entity, "id", id);
        setField(entity, "priority", priority);
        setField(entity, "conditionJson", conditionJson);
        setField(entity, "approverType", approverType);
        return entity;
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
