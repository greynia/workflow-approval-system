package com.eva.workflow.approval.infrastructure.security;

public final class SecurityConstants {

    /**
     * HttpOnly cookie name for JWT token.
     * Named with an app-specific prefix to avoid conflicts if multiple services
     * are deployed on the same domain (e.g. an admin panel alongside this app).
     */
    public static final String TOKEN_COOKIE_NAME = "workflow-token";

    private SecurityConstants() {}
}
