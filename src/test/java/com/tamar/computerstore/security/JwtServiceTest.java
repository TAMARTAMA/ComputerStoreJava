package com.tamar.computerstore.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tamar.computerstore.config.JwtProperties;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hs256-0123456789";
    private static final String ISSUER = "computer-store-api";

    private final JwtService jwtService = new JwtService(
            new JwtProperties(SECRET, Duration.ofMinutes(15), ISSUER)
    );

    @Test
    void generatesTokenCarryingSubjectRoleAndIssuer() {
        String token = jwtService.generateToken(account());

        Jwt decoded = jwtService.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("admin@example.com");
        assertThat(decoded.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo(UserRole.ADMIN.name());
        Object userId = decoded.getClaim(JwtService.USER_ID_CLAIM);
        assertThat(userId).hasToString("42");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
        assertThat(token).doesNotContain("not-a-real-hash");
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService attacker = new JwtService(new JwtProperties(
                "a-completely-different-secret-of-sufficient-length-1234", Duration.ofMinutes(15), ISSUER
        ));
        String forged = attacker.generateToken(account());

        assertThatThrownBy(() -> jwtService.decode(forged)).isInstanceOf(JwtException.class);
    }

    /**
     * The encoder refuses to mint an already-expired token, so the fixture is signed with raw
     * Nimbus using the same secret. Only the decoder is under test here.
     */
    @Test
    void rejectsExpiredToken() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject("admin@example.com")
                .issueTime(Date.from(now.minus(Duration.ofHours(2))))
                .expirationTime(Date.from(now.minus(Duration.ofHours(1))))
                .build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signed.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        String expired = signed.serialize();

        assertThatThrownBy(() -> jwtService.decode(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtService otherIssuer = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15), "some-other-api"));
        String token = otherIssuer.generateToken(account());

        assertThatThrownBy(() -> jwtService.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(account());

        assertThatThrownBy(() -> jwtService.decode(token + "x")).isInstanceOf(JwtException.class);
    }

    private static UserAccount account() {
        UserAccount account = new UserAccount("admin@example.com", "not-a-real-hash", UserRole.ADMIN);
        account.setId(42L);
        return account;
    }
}
