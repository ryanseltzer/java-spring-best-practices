package learn.spring_best_practices.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String SECRET =
            "dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZw==";

    @InjectMocks
    JwtUtil jwtUtil;

    @BeforeEach
    void injectSecret() {
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
    }

    // ── isTokenValid ─────────────────────────────────────────────────────

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken("alice", futureDate(60_000));
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String token = buildToken("alice", pastDate(60_000));
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_tamperedSignature_returnsFalse() {
        String token = buildToken("alice", futureDate(60_000)) + "tampered";
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_garbage_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.jwt.at.all")).isFalse();
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    // ── extractSubject ────────────────────────────────────────────────────

    @Test
    void extractSubject_validToken_returnsSubject() {
        String token = buildToken("testuser", futureDate(60_000));
        assertThat(jwtUtil.extractSubject(token)).isEqualTo("testuser");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String buildToken(String subject, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    private Date futureDate(long offsetMs) {
        return new Date(System.currentTimeMillis() + offsetMs);
    }

    private Date pastDate(long offsetMs) {
        return new Date(System.currentTimeMillis() - offsetMs);
    }
}
