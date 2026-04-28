package com.eva.workflow.approval.application.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eva.workflow.approval.application.exception.InvalidCredentialsApplicationException;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.EmployeeEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.RefreshTokenEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.RefreshTokenRepository;
import com.eva.workflow.approval.infrastructure.security.JwtProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public IssuedRefreshToken issue(EmployeeEntity employee) {
        return issue(employee, UUID.randomUUID());
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsApplicationException("Invalid refresh token"));

        LocalDateTime now = now();
        if (current.isRevoked()) {
            revokeFamily(current.getFamilyId(), now);
            throw new InvalidCredentialsApplicationException("Refresh token replay detected");
        }
        if (current.isExpired(now)) {
            current.revoke(now);
            throw new InvalidCredentialsApplicationException("Refresh token expired");
        }

        IssuedRefreshToken next = issue(current.getEmployee(), current.getFamilyId());
        RefreshTokenEntity replacement = refreshTokenRepository.findByTokenHash(hash(next.token()))
                .orElseThrow(() -> new IllegalStateException("Replacement refresh token not found"));
        current.markRotated(replacement.getId(), now);
        return next;
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(now()));
    }

    private IssuedRefreshToken issue(EmployeeEntity employee, UUID familyId) {
        LocalDateTime issuedAt = now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(jwtProperties.refreshExpirationSeconds());
        String rawToken = generateOpaqueToken();
        RefreshTokenEntity entity = RefreshTokenEntity.issue(
                employee,
                hash(rawToken),
                familyId,
                expiresAt,
                issuedAt
        );
        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(employee, rawToken, expiresAt);
    }

    private void revokeFamily(UUID familyId, LocalDateTime now) {
        List<RefreshTokenEntity> familyTokens = refreshTokenRepository.findByFamilyId(familyId);
        for (RefreshTokenEntity token : familyTokens) {
            token.revoke(now);
        }
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
    }
}
