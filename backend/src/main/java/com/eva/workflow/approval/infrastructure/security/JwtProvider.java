package com.eva.workflow.approval.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.eva.workflow.approval.application.auth.AuthenticatedEmployee;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = buildSecretKey(jwtProperties.secret());
    }

    public String generateToken(EmployeeEntity employee) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.expirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(employee.getId()))
                .claim("email", employee.getEmail())
                .claim("name", employee.getName())
                .claim("role", employee.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedEmployee parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedEmployee(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("name", String.class),
                com.eva.workflow.approval.common.enums.UserRole.valueOf(claims.get("role", String.class))
        );
    }

    private SecretKey buildSecretKey(String secret) {
        byte[] rawSecret = secret.getBytes(StandardCharsets.UTF_8);

        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (DecodingException ignored) {
            // Fall back to treating the configured value as a plain-text secret.
        }

        if (rawSecret.length >= 32) {
            return Keys.hmacShaKeyFor(rawSecret);
        }

        throw new IllegalArgumentException("JWT secret must be at least 32 bytes as plain text or Base64-decoded key material");
    }
}
