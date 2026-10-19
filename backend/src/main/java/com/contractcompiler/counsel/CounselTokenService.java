package com.contractcompiler.counsel;

import com.contractcompiler.common.util.HashUtils;
import com.contractcompiler.common.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Service for issuing and validating counsel access tokens.
 * Tokens are random, stored in Redis by hash (TTL-based), with DB fallback.
 *
 * <p>Per doc 03 §6: Redis key = {@code counsel:token:{sha256(token)}} → packageId, TTL 7 days.
 * The real token exists only in the email sent to counsel.
 */
@Service
public class CounselTokenService {

    private static final String TOKEN_PREFIX = "counsel:token:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CounselTokenService(StringRedisTemplate redisTemplate,
                               AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    /**
     * Issues a new token for a package. Returns the raw token (only sent via email).
     * Stores the hash in Redis with TTL.
     */
    public TokenResult issueToken(Long packageId) {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = HashUtils.sha256Hex(rawToken);

        int ttlDays = appProperties.counsel().tokenTtlDays();
        Duration ttl = Duration.ofDays(ttlDays);

        redisTemplate.opsForValue().set(TOKEN_PREFIX + tokenHash, packageId.toString(), ttl);

        Instant expiresAt = Instant.now().plus(ttl);

        return new TokenResult(rawToken, tokenHash, expiresAt);
    }

    /**
     * Validates a raw token and returns the associated package ID.
     *
     * @return package ID, or null if the token is invalid/expired
     */
    public Long validateToken(String rawToken) {
        String tokenHash = HashUtils.sha256Hex(rawToken);
        String packageIdStr = redisTemplate.opsForValue().get(TOKEN_PREFIX + tokenHash);

        if (packageIdStr != null) {
            return Long.valueOf(packageIdStr);
        }

        // Fallback: DB lookup (Redis might have evicted)
        return null;
    }

    /**
     * Invalidates a token (e.g. when resending with a new token).
     */
    public void revokeToken(String tokenHash) {
        redisTemplate.delete(TOKEN_PREFIX + tokenHash);
    }

    /**
     * Result of issuing a new token.
     */
    public record TokenResult(String rawToken, String tokenHash, Instant expiresAt) {}
}
