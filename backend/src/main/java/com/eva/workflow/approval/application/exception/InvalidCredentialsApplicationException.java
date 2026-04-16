package com.eva.workflow.approval.application.exception;

public class InvalidCredentialsApplicationException extends ApplicationException {

    public InvalidCredentialsApplicationException(String message) {
        super("INVALID_CREDENTIALS", message);
    }
}
