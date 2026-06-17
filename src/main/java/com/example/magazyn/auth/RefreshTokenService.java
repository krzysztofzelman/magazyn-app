package com.example.magazyn.auth;

import com.example.magazyn.entity.RefreshToken;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh-expiration:604800000}") long refreshTokenDurationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    /**
     * Generates a random UUID refresh token, stores only its SHA-256 hash,
     * and returns the raw token string to be sent to the client.
     */
    @Transactional
    public RefreshTokenResult generateRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        return new RefreshTokenResult(rawToken, saved);
    }

    /**
     * Validates a refresh token, marks it as used, and generates a new one (rotation).
     * <p>
     * <b>Reuse detection:</b> if the token was already marked as used (someone else
     * previously rotated it), ALL tokens for this user are deleted — the old token
     * may have been stolen.
     *
     * @param oldTokenStr the raw refresh token to rotate
     * @return the user who owns the token
     * @throws RefreshTokenException if the token is invalid, expired, or reused
     */
    @Transactional
    public RotateResult rotate(String oldTokenStr) {
        String tokenHash = sha256Hex(oldTokenStr);

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenException("Token odświeżania nie istnieje"));

        if (oldToken.isExpired()) {
            refreshTokenRepository.delete(oldToken);
            throw new RefreshTokenException("Token odświeżania wygasł");
        }

        if (oldToken.isUsed()) {
            // Reuse detected — someone else already rotated this token.
            // Delete ALL tokens for the user as a security measure.
            User owner = oldToken.getUser();
            refreshTokenRepository.deleteByUser(owner);
            throw new RefreshTokenException(
                    "Token odświeżania został już użyty — wszystkie tokeny unieważnione");
        }

        // Mark old token as used (rotation)
        oldToken.setUsed(true);
        refreshTokenRepository.save(oldToken);

        User user = oldToken.getUser();

        // Generate new token
        String rawToken = UUID.randomUUID().toString();
        String newTokenHash = sha256Hex(rawToken);

        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken saved = refreshTokenRepository.save(newToken);
        return new RotateResult(rawToken, saved);
    }

    /**
     * Deletes all refresh tokens for the given user (e.g. on logout or reuse detection).
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Invalidates all tokens for the user identified by the given refresh token string.
     * If the token no longer exists (already rotated, expired, or invalid),
     * this is a no-op — the user is effectively logged out either way.
     */
    @Transactional
    public void logout(String rawTokenStr) {
        String tokenHash = sha256Hex(rawTokenStr);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> deleteByUser(token.getUser()));
    }

    /**
     * Clean up used tokens older than 24 hours to prevent table bloat.
     * Runs once daily at 03:00.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupUsedTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        refreshTokenRepository.findByUsedTrueAndCreatedAtBefore(cutoff)
                .forEach(refreshTokenRepository::delete);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Result containing the raw token (to send to client) and the saved entity.
     */
    public record RefreshTokenResult(String rawToken, RefreshToken entity) {}

    /**
     * Result of a rotation: the new raw token (to send to client) and the new entity.
     */
    public record RotateResult(String rawToken, RefreshToken entity) {}
}
