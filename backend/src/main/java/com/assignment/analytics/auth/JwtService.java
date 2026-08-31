package com.assignment.analytics.auth;

import com.assignment.analytics.config.AppSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppSecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(properties.jwtSecret()));
        this.ttl = Duration.ofMinutes(properties.jwtTtlMinutes());
    }

    public String issueToken(UUID operatorId, String username, String displayName, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("operatorId", operatorId.toString())
                .claim("displayName", displayName)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Returns the token claims if the token is valid and not expired. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
