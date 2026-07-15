package com.wolffsoft.blogapi.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.wolffsoft.blogapi.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_ROLES;
import static com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_TOKEN_TYPE;
import static com.wolffsoft.blogapi.auth.TokenClaims.TYPE_ACCESS;
import static com.wolffsoft.blogapi.auth.TokenClaims.TYPE_REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Uses a REAL {@link NimbusJwtEncoder}/{@link NimbusJwtDecoder} pair (built exactly the way
 * {@link SecurityConfig} builds them) so tokens are round-tripped through actual JWT encoding
 * and decoding, rather than mocking JwtEncoder and only proving it was called.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final String TEST_SECRET = "test-secret-key-at-least-32-characters-long";
    private static final Duration ACCESS_TTL = Duration.ofSeconds(900);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    @Mock
    private User user;

    private JwtDecoder jwtDecoder;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        SecretKeySpec secretKey = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ACCESS_TTL, REFRESH_TTL);
        tokenService = new TokenService(jwtEncoder, jwtProperties);
    }

    @Test
    @DisplayName("issueAccessToken produces a token with the user's subject, an access token_type, and ROLE_USER for a non-admin user")
    void issueAccessTokenNonAdminUserProducesAccessTokenWithUserRole() {
        // given
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getIsAdmin()).thenReturn(false);
        AppUserDetails userDetails = AppUserDetails.from(user);

        // when
        String token = tokenService.issueAccessToken(userDetails);
        Jwt jwt = jwtDecoder.decode(token);

        // then
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString(CLAIM_TOKEN_TYPE)).isEqualTo(TYPE_ACCESS);
        assertThat(jwt.getClaimAsStringList(CLAIM_ROLES)).containsExactly("ROLE_USER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(ACCESS_TTL);
    }

    @Test
    @DisplayName("issueAccessToken produces a token with ROLE_ADMIN for an admin user")
    void issueAccessTokenAdminUserProducesAccessTokenWithAdminRole() {
        // given
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("admin@example.com");
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getIsAdmin()).thenReturn(true);
        AppUserDetails userDetails = AppUserDetails.from(user);

        // when
        String token = tokenService.issueAccessToken(userDetails);
        Jwt jwt = jwtDecoder.decode(token);

        // then
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString(CLAIM_TOKEN_TYPE)).isEqualTo(TYPE_ACCESS);
        assertThat(jwt.getClaimAsStringList(CLAIM_ROLES)).containsExactly("ROLE_ADMIN");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(ACCESS_TTL);
    }

    @Test
    @DisplayName("issueRefreshToken produces a minimal token with a refresh token_type and no roles claim")
    void issueRefreshTokenProducesRefreshTokenWithoutRolesClaim() {
        // given
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getIsAdmin()).thenReturn(false);
        AppUserDetails userDetails = AppUserDetails.from(user);

        // when
        String token = tokenService.issueRefreshToken(userDetails);
        Jwt jwt = jwtDecoder.decode(token);

        // then
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString(CLAIM_TOKEN_TYPE)).isEqualTo(TYPE_REFRESH);
        assertThat(jwt.getClaims()).doesNotContainKey(CLAIM_ROLES);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(REFRESH_TTL);
    }
}