package com.eva.workflow.approval.application.exception;

public class ApplicationConfigurationException extends ApplicationException {

    public ApplicationConfigurationException(String message) {
        super("APPLICATION_CONFIGURATION_ERROR", message);
    }
}
