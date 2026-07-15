package com.wolffsoft.blogapi.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TokenClaims {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String CLAIM_ROLES = "roles";
}
