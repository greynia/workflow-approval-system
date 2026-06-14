package com.eva.workflow.approval.application.exception;

public class BadRequestApplicationException extends ApplicationException {

    public BadRequestApplicationException(String message) {
        super("BAD_REQUEST", message);
    }
}
