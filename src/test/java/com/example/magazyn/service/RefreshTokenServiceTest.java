package com.example.magazyn.service;

import com.example.magazyn.auth.RefreshTokenException;
import com.example.magazyn.auth.RefreshTokenService;
import com.example.magazyn.auth.RefreshTokenService.RefreshTokenResult;
import com.example.magazyn.entity.RefreshToken;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 604800000L);
    }

    private User createUser(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .role("ROLE_USER")
                .build();
    }

    private RefreshToken createToken(Long id, String tokenHash, User user, boolean expired) {
        return RefreshToken.builder()
                .id(id)
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(expired ? LocalDateTime.now().minusDays(1) : LocalDateTime.now().plusDays(6))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ──────────────────────────────────────────────
    // generateRefreshToken
    // ──────────────────────────────────────────────

    @Test
    void generateRefreshToken_createsAndSaves() {
        User user = createUser(1L, "testuser");

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken saved = i.getArgument(0);
            return RefreshToken.builder()
                    .id(1L)
                    .tokenHash(saved.getTokenHash())
                    .user(saved.getUser())
                    .expiresAt(saved.getExpiresAt())
                    .createdAt(saved.getCreatedAt())
                    .build();
        });

        RefreshTokenResult result = refreshTokenService.generateRefreshToken(user);

        assertNotNull(result);
        assertNotNull(result.rawToken());
        assertNotNull(result.entity());
        assertNotNull(result.entity().getId());
        assertEquals(user.getId(), result.entity().getUser().getId());
        assertTrue(result.entity().getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(result.entity().getExpiresAt().isBefore(LocalDateTime.now().plusDays(8)));
        assertNotNull(result.entity().getCreatedAt());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void generateRefreshToken_tokenIsUUID() {
        User user = createUser(1L, "testuser");

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken saved = i.getArgument(0);
            return RefreshToken.builder()
                    .id(1L)
                    .tokenHash(saved.getTokenHash())
                    .user(saved.getUser())
                    .expiresAt(saved.getExpiresAt())
                    .createdAt(saved.getCreatedAt())
                    .build();
        });

        RefreshTokenResult result = refreshTokenService.generateRefreshToken(user);

        assertDoesNotThrow(() -> UUID.fromString(result.rawToken()));
        assertNotNull(result.rawToken());
    }

    // ──────────────────────────────────────────────
    // rotate — success
    // ──────────────────────────────────────────────

    @Test
    void rotate_validToken_returnsRotateResult() {
        User user = createUser(1L, "testuser");
        RefreshToken refreshToken = createToken(1L, "some-hash", user, false);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshTokenService.RotateResult result = refreshTokenService.rotate("any-raw-token");

        assertNotNull(result);
        assertNotNull(result.rawToken());
        assertNotNull(result.entity());
        assertTrue(result.entity().getExpiresAt().isAfter(LocalDateTime.now()));

        // Old token should be marked as used
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        assertTrue(refreshToken.isUsed());
    }

    // ──────────────────────────────────────────────
    // rotate — errors
    // ──────────────────────────────────────────────

    @Test
    void rotate_expiredToken_throwsAndDeletes() {
        User user = createUser(1L, "testuser");
        RefreshToken expiredToken = createToken(1L, "some-hash", user, true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        RefreshTokenException ex = assertThrows(RefreshTokenException.class,
                () -> refreshTokenService.rotate("any-raw-token"));

        assertTrue(ex.getMessage().contains("wygasł"));
        verify(refreshTokenRepository).findByTokenHash(anyString());
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void rotate_nonexistentToken_throws() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RefreshTokenException ex = assertThrows(RefreshTokenException.class,
                () -> refreshTokenService.rotate("any-raw-token"));

        assertTrue(ex.getMessage().contains("nie istnieje"));
        verify(refreshTokenRepository).findByTokenHash(anyString());
    }

    @Test
    void rotate_reusedToken_throwsAndDeletesAllForUser() {
        User user = createUser(1L, "testuser");
        RefreshToken usedToken = createToken(1L, "some-hash", user, false);
        usedToken.setUsed(true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

        RefreshTokenException ex = assertThrows(RefreshTokenException.class,
                () -> refreshTokenService.rotate("any-raw-token"));

        assertTrue(ex.getMessage().contains("już użyty"));
        verify(refreshTokenRepository).findByTokenHash(anyString());
        verify(refreshTokenRepository).deleteByUser(user);
    }

    // ──────────────────────────────────────────────
    // deleteByUser
    // ──────────────────────────────────────────────

    @Test
    void deleteByUser_callsRepository() {
        User user = createUser(1L, "testuser");

        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}
