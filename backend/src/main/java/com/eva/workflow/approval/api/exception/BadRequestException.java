package com.eva.workflow.approval.api.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
}
