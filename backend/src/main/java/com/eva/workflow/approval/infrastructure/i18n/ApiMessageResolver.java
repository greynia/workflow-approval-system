package com.eva.workflow.approval.infrastructure.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.eva.workflow.approval.application.exception.ApplicationException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiMessageResolver {

    private final MessageSource messageSource;

    public String resolve(String key, Object[] args, String defaultMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, defaultMessage, locale);
    }

    public String resolveApplicationMessage(ApplicationException exception) {
        String rawMessage = exception.getMessage();
        String codeKey = "error." + exception.getCode();
        String specificKey = mapRawMessageKey(rawMessage);

        if (specificKey != null) {
            return resolve(specificKey, null, rawMessage);
        }
        return resolve(codeKey, null, rawMessage);
    }

    private String mapRawMessageKey(String rawMessage) {
        return switch (rawMessage) {
            case "Invalid email or password" -> "error.INVALID_CREDENTIALS";
            case "Invalid refresh token" -> "error.refresh.invalid";
            case "Refresh token replay detected" -> "error.refresh.replay";
            case "Refresh token expired" -> "error.refresh.expired";
            case "Leave request not found" -> "error.leaveRequest.notFound";
            case "Approval step not found" -> "error.approvalStep.notFound";
            case "Current employee not found" -> "error.employee.currentNotFound";
            case "Employee not found" -> "error.employee.notFound";
            case "Applicant not found" -> "error.employee.applicantNotFound";
            case "Deputy not found" -> "error.employee.deputyNotFound";
            case "Company work schedule not found" -> "error.companySchedule.notFound";
            case "You are not allowed to cancel this leave request" -> "error.leaveRequest.cancelForbidden";
            case "You are not allowed to process this approval step" -> "error.approvalStep.forbidden";
            case "Only pending leave requests can be cancelled" -> "error.leaveRequest.cancelPendingOnly";
            case "Approval step has already been processed" -> "error.approvalStep.alreadyProcessed";
            case "Previous approval step has not been completed" -> "error.approvalStep.previousIncomplete";
            case "deputyId is required" -> "error.leaveRequest.deputyRequired";
            case "deputyId cannot be the same as applicant" -> "error.leaveRequest.deputySameAsApplicant";
            case "endTime must be after startTime" -> "error.leaveRequest.invalidTimeRange";
            case "APPLICANT_ON_LEAVE" -> "error.leaveRequest.applicantOnLeave";
            case "DEPUTY_ON_LEAVE" -> "error.leaveRequest.deputyOnLeave";
            case "INVALID_DURATION_UNIT" -> "error.leaveRequest.invalidDurationUnit";
            case "UNSUPPORTED_EMPLOYEE_SCHEDULE" -> "error.leaveRequest.unsupportedEmployeeSchedule";
            default -> null;
        };
    }
}
