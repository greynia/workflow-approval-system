package com.eva.workflow.approval.application.exception;

public class ForbiddenApplicationException extends ApplicationException {

    public ForbiddenApplicationException(String message) {
        super("FORBIDDEN", message);
    }
}
