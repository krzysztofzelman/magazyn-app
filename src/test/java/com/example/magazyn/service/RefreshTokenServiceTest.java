package com.example.magazyn.service;

import com.example.magazyn.auth.RefreshTokenException;
import com.example.magazyn.auth.RefreshTokenService;
import com.example.magazyn.entity.RefreshToken;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Captor
    private ArgumentCaptor<RefreshToken> tokenCaptor;

    private User createUser(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .role("ROLE_USER")
                .build();
    }

    private RefreshToken createToken(Long id, UUID tokenUuid, User user, boolean expired) {
        return RefreshToken.builder()
                .id(id)
                .token(tokenUuid)
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
                    .token(saved.getToken())
                    .user(saved.getUser())
                    .expiresAt(saved.getExpiresAt())
                    .createdAt(saved.getCreatedAt())
                    .build();
        });

        RefreshToken result = refreshTokenService.generateRefreshToken(user);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getToken());
        assertEquals(user.getId(), result.getUser().getId());
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(result.getExpiresAt().isBefore(LocalDateTime.now().plusDays(8)));
        assertNotNull(result.getCreatedAt());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void generateRefreshToken_tokenIsUUID() {
        User user = createUser(1L, "testuser");

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken saved = i.getArgument(0);
            return RefreshToken.builder()
                    .id(1L)
                    .token(saved.getToken())
                    .user(saved.getUser())
                    .expiresAt(saved.getExpiresAt())
                    .createdAt(saved.getCreatedAt())
                    .build();
        });

        RefreshToken result = refreshTokenService.generateRefreshToken(user);

        assertDoesNotThrow(() -> UUID.fromString(result.getToken().toString()));
        assertNotNull(result.getToken());
    }

    // ──────────────────────────────────────────────
    // validateRefreshToken — success
    // ──────────────────────────────────────────────

    @Test
    void validateRefreshToken_validToken_returnsUser() {
        User user = createUser(1L, "testuser");
        UUID tokenUuid = UUID.randomUUID();
        RefreshToken refreshToken = createToken(1L, tokenUuid, user, false);

        when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(refreshToken));

        User result = refreshTokenService.validateRefreshToken(tokenUuid.toString());

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());

        verify(refreshTokenRepository).findByToken(tokenUuid);
        verify(refreshTokenRepository, never()).delete(any());
    }

    // ──────────────────────────────────────────────
    // validateRefreshToken — errors
    // ──────────────────────────────────────────────

    @Test
    void validateRefreshToken_expiredToken_throwsAndDeletes() {
        User user = createUser(1L, "testuser");
        UUID tokenUuid = UUID.randomUUID();
        RefreshToken expiredToken = createToken(1L, tokenUuid, user, true);

        when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(expiredToken));

        RefreshTokenException ex = assertThrows(RefreshTokenException.class,
                () -> refreshTokenService.validateRefreshToken(tokenUuid.toString()));

        assertTrue(ex.getMessage().contains("wygasł"));
        verify(refreshTokenRepository).findByToken(tokenUuid);
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void validateRefreshToken_invalidUUID_throws() {
        RefreshTokenException ex = assertThrows(RefreshTokenException.class,
                () -> refreshTokenService.validateRefreshToken("not-a-uuid"));

        assertTrue(ex.getMessage().contains("format"));
        verify(refreshTokenRepository, never()).findByToken(any());
    }

    @Test
    void validateRefreshToken_nonexistentToken_throws() {
        UUID tokenUuid = UUID.randomUUID();

        when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.empty());

        RefreshTokenException ex = assertThrows(RefreshTokenException.class,
                () -> refreshTokenService.validateRefreshToken(tokenUuid.toString()));

        assertTrue(ex.getMessage().contains("nie istnieje"));
        verify(refreshTokenRepository).findByToken(tokenUuid);
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
