package br.pucpr.authserver.security;

import br.pucpr.authserver.config.AppProperties;
import io.jsonwebtoken.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final AppProperties.Security properties;
    private final Clock clock;
    private final SecretKey key;
    private final JwtParser parser;

    public JwtService(AppProperties properties, Clock clock) {
        this.properties = properties.security();
        this.clock = clock;
        this.key = new SecretKeySpec(this.properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.parser = Jwts.parser().verifyWith(key).requireIssuer(this.properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .sig().clear().add(Jwts.SIG.HS256).and().build();
    }

    public String issue(AuthUser user) {
        Instant now = clock.instant();
        var ttl = user.roles().contains("ADMIN") ? properties.adminTokenTtl() : properties.tokenTtl();
        return Jwts.builder().issuer(properties.issuer()).subject(user.id().toString())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl)))
                .claim("user", Map.of("id", user.id(), "name", user.name(), "roles", user.roles()))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public AuthUser parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        try {
            if (claims.getIssuedAt() == null || claims.getExpiration() == null
                    || claims.getIssuedAt().after(Date.from(clock.instant()))
                    || !claims.getExpiration().after(claims.getIssuedAt())) {
                throw new IllegalArgumentException();
            }
            long id = Long.parseLong(claims.getSubject());
            if (!(claims.get("user") instanceof Map<?, ?> user)
                    || !(user.get("id") instanceof Number claimId)
                    || !Long.toString(id).equals(claimId.toString())
                    || id < 1 || !(user.get("name") instanceof String name) || name.isBlank()
                    || !(user.get("roles") instanceof List<?> roles)
                    || roles.stream().anyMatch(role -> !(role instanceof String text) || text.isBlank())) {
                throw new IllegalArgumentException();
            }
            return new AuthUser(id, name, roles.stream().map(String.class::cast).toList());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new JwtException("Claims inválidas.");
        }
    }
}
