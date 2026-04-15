package com.eva.workflow.approval.application.exception;

public class ResourceNotFoundApplicationException extends ApplicationException {

    public ResourceNotFoundApplicationException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
