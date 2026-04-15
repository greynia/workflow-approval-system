package com.eva.workflow.approval.application.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.application.exception.ApplicationConfigurationException;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class ManagerChainResolverTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private ManagerChainResolver managerChainResolver;

    @BeforeEach
    void setUp() {
        managerChainResolver = new ManagerChainResolver(employeeRepository);
    }

    @Test
    void resolveEligibleManagerChainIdsKeepsOrderAndFiltersInactiveManagers() {
        EmployeeEntity applicant = employee(7L, 5L, true);

        when(employeeRepository.findManagerChainRows(7L)).thenReturn(List.of(
                row(5L, true, false),
                row(2L, false, false),
                row(1L, true, false)
        ));

        List<Long> chain = managerChainResolver.resolveEligibleManagerChainIds(applicant);

        assertThat(chain).containsExactly(5L, 1L);
    }

    @Test
    void resolveEligibleManagerChainIdsFailsWhenManagerHierarchyContainsCycle() {
        EmployeeEntity applicant = employee(7L, 2L, true);

        when(employeeRepository.findManagerChainRows(7L)).thenReturn(List.of(
                row(2L, true, false),
                row(1L, true, false),
                row(2L, true, true)
        ));

        assertThatThrownBy(() -> managerChainResolver.resolveEligibleManagerChainIds(applicant))
                .isInstanceOf(ApplicationConfigurationException.class)
                .hasMessageContaining("Manager cycle detected");
    }

    @Test
    void resolveEligibleManagerChainIdsReturnsEmptyWhenApplicantHasNoManager() {
        EmployeeEntity applicant = employee(7L, null, true);

        when(employeeRepository.findManagerChainRows(7L)).thenReturn(List.of());

        assertThat(managerChainResolver.resolveEligibleManagerChainIds(applicant)).isEmpty();
    }

    private EmployeeEntity employee(Long id, Long managerId, boolean active) {
        EmployeeEntity entity = newInstance(EmployeeEntity.class);
        setField(entity, "id", id);
        setField(entity, "active", active);
        if (managerId != null) {
            EmployeeEntity manager = newInstance(EmployeeEntity.class);
            setField(manager, "id", managerId);
            setField(entity, "manager", manager);
        }
        return entity;
    }

    private EmployeeRepository.ManagerChainRow row(Long employeeId, boolean active, boolean cycle) {
        return new EmployeeRepository.ManagerChainRow() {
            @Override
            public Long getEmployeeId() {
                return employeeId;
            }

            @Override
            public Boolean getActive() {
                return active;
            }

            @Override
            public Boolean getCycle() {
                return cycle;
            }
        };
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
