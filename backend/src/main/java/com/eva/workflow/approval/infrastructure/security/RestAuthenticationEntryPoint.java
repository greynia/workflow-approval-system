package com.eva.workflow.approval.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.eva.workflow.approval.api.dto.common.ErrorResponse;
import com.eva.workflow.approval.infrastructure.i18n.ApiMessageResolver;
import com.eva.workflow.approval.infrastructure.observability.RequestCorrelationConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ApiMessageResolver messageResolver;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        Object requestId = request.getAttribute(RequestCorrelationConstants.REQUEST_ID_ATTRIBUTE);
        String correlationId = requestId != null ? requestId.toString() : null;

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse(
                        "UNAUTHORIZED",
                        messageResolver.resolve("error.UNAUTHORIZED", null, "Authentication is required"),
                        correlationId
                )
        );
    }
}
