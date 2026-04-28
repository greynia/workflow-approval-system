package com.eva.workflow.approval.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public static RefreshTokenEntity issue(
            EmployeeEntity employee,
            String tokenHash,
            UUID familyId,
            LocalDateTime expiresAt,
            LocalDateTime issuedAt
    ) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = UUID.randomUUID();
        entity.employee = employee;
        entity.tokenHash = tokenHash;
        entity.familyId = familyId;
        entity.expiresAt = expiresAt;
        entity.createdAt = issuedAt;
        entity.lastUsedAt = issuedAt;
        return entity;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now) || expiresAt.isEqual(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void markRotated(UUID replacementTokenId, LocalDateTime now) {
        this.replacedByTokenId = replacementTokenId;
        this.revokedAt = now;
        this.lastUsedAt = now;
    }

    public void revoke(LocalDateTime now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
        this.lastUsedAt = now;
    }

    public void touch(LocalDateTime now) {
        this.lastUsedAt = now;
    }
}
