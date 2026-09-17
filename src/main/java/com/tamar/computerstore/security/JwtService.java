package com.tamar.computerstore.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.tamar.computerstore.config.JwtProperties;
import com.tamar.computerstore.entity.UserAccount;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues and verifies HS256 access tokens. Verification enforces signature, issuer, and
 * expiry; a small clock skew is tolerated so a slightly fast client is not locked out.
 */
@Service
public class JwtService {

    public static final String ROLE_CLAIM = "role";
    public static final String USER_ID_CLAIM = "uid";

    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;
    private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofSeconds(30);

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        SecretKey key = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM.getName()
        );
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(ALGORITHM)
                .build();
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(ALLOWED_CLOCK_SKEW),
                new JwtIssuerValidator(properties.issuer())
        ));
        this.decoder = nimbusDecoder;
    }

    public String generateToken(UserAccount account) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(account.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.expiration()))
                .claim(USER_ID_CLAIM, account.getId())
                .claim(ROLE_CLAIM, account.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(ALGORITHM).build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * @throws JwtException if the token is malformed, expired, or not signed with our key
     */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }
}
