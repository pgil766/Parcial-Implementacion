package edu.eia.racing.security;

import edu.eia.racing.model.enums.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates the HMAC-signed JWTs used for stateless authentication.
 *
 * <p>Two token types are issued: a short-lived {@code access} token sent on every
 * request, and a long-lived {@code refresh} token accepted only by
 * {@code POST /api/auth/refresh}. The {@code type} claim keeps them from being
 * used interchangeably.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        // HS256 requires at least 256 bits of key material; a short JWT_SECRET fails here on startup.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String username, RoleName role) {
        return buildToken(username, role, TYPE_ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(String username, RoleName role) {
        return buildToken(username, role, TYPE_REFRESH, refreshExpirationMs);
    }

    /** Access-token lifetime in seconds, for the {@code expiresIn} field of the auth response. */
    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /** Returns the claims only when the token is a valid, unexpired access token. */
    public Optional<Claims> parseAccessToken(String token) {
        return parseToken(token, TYPE_ACCESS);
    }

    /** Returns the claims only when the token is a valid, unexpired refresh token. */
    public Optional<Claims> parseRefreshToken(String token) {
        return parseToken(token, TYPE_REFRESH);
    }

    private String buildToken(String username, RoleName role, String type, long ttlMs) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(ttlMs)))
                .signWith(signingKey)
                .compact();
    }

    private Optional<Claims> parseToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return expectedType.equals(claims.get(CLAIM_TYPE, String.class))
                    ? Optional.of(claims)
                    : Optional.empty();
        } catch (JwtException | IllegalArgumentException ex) {
            // Bad signature, malformed token or expired token: all mean "not authenticated".
            return Optional.empty();
        }
    }
}
