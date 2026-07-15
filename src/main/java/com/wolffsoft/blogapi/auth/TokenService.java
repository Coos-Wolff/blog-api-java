package com.wolffsoft.blogapi.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_ROLES;
import static com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_TOKEN_TYPE;
import static com.wolffsoft.blogapi.auth.TokenClaims.TYPE_ACCESS;
import static com.wolffsoft.blogapi.auth.TokenClaims.TYPE_REFRESH;


@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public TokenService(final JwtEncoder jwtEncoder, final JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String issueAccessToken(AppUserDetails user) {
        return getToken(TYPE_ACCESS, user, jwtProperties.accessTokenTtl());
    }

    public String issueRefreshToken(AppUserDetails user) {
        return getToken(TYPE_REFRESH, user, jwtProperties.refreshTokenTtl());
    }

    private String getToken(String tokenType, AppUserDetails user, Duration expiresIn) {
        JwsHeader jwsHeader = getJwsHeader();
        JwtClaimsSet jwtClaimsSet = getJwtClaimsSet(tokenType, user, expiresIn);
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, jwtClaimsSet)).getTokenValue();
    }

    private JwsHeader getJwsHeader() {
        return JwsHeader.with(MacAlgorithm.HS256).build();
    }

    private JwtClaimsSet getJwtClaimsSet(String tokenType, AppUserDetails user, Duration expiresIn) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(expiresIn))
                .claim(CLAIM_TOKEN_TYPE, tokenType);

        addRoleIfRoleAccess(tokenType, builder, user);

        return builder.build();
    }

    private List<String> getAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private void addRoleIfRoleAccess(String tokenType, JwtClaimsSet.Builder builder, AppUserDetails user) {
        if (TYPE_ACCESS.equals(tokenType)) {
            List<String> authorities = getAuthorities(user.getAuthorities());
            builder.claim(CLAIM_ROLES, authorities);
        }
    }
}
