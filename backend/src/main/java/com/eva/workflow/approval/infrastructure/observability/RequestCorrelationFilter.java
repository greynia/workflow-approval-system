package com.eva.workflow.approval.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-200)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader(RequestCorrelationConstants.REQUEST_ID_HEADER))
                .map(id -> id.replaceAll("[\\r\\n]", ""))
                .filter(id -> !id.isBlank() && id.length() <= 64)
                .orElse(UUID.randomUUID().toString());

        request.setAttribute(RequestCorrelationConstants.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(RequestCorrelationConstants.REQUEST_ID_HEADER, requestId);
        MDC.put(RequestCorrelationConstants.MDC_KEY, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(RequestCorrelationConstants.MDC_KEY);
        }
    }
}
