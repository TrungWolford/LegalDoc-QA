package com.contractcompiler.auth;

import com.contractcompiler.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service for JWT token generation and validation.
 * Uses HMAC-SHA256 with a secret from configuration.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(AppProperties appProperties) {
        String secret = appProperties.security().jwt().secret();
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = appProperties.security().jwt().expirationMs();
    }

    /**
     * Generates a JWT token for the given user.
     */
    public String generateToken(AppUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT token, returning the claims.
     *
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the email (subject) from a token.
     */
    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }
}
