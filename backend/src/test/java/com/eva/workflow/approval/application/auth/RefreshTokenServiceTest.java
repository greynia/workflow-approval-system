package com.eva.workflow.approval.application.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eva.workflow.approval.application.exception.InvalidCredentialsApplicationException;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.RefreshTokenRepository;
import com.eva.workflow.approval.infrastructure.security.JwtProperties;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                new JwtProperties("01234567890123456789012345678901", 900, 1209600),
                Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void rotateRejectsMissingTokenBeforeHashing() {
        assertThatThrownBy(() -> refreshTokenService.rotate(null))
                .isInstanceOf(InvalidCredentialsApplicationException.class)
                .hasMessage("Invalid refresh token");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void rotateRejectsBlankTokenBeforeHashing() {
        assertThatThrownBy(() -> refreshTokenService.rotate(" "))
                .isInstanceOf(InvalidCredentialsApplicationException.class)
                .hasMessage("Invalid refresh token");

        verifyNoInteractions(refreshTokenRepository);
    }
}
