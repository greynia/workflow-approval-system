package com.eva.workflow.approval.api.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super("BUSINESS_RULE_ERROR", message, HttpStatus.BAD_REQUEST);
    }
}
