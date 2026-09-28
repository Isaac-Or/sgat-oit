package pe.gob.cultura.sgat.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expiracionMinutos;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(Long usuarioId, String correo, List<String> roles) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim("uid", usuarioId)
                .claim("roles", roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Valida la firma y la expiracion; lanza JwtException si el token no es valido. */
    public Claims leer(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
