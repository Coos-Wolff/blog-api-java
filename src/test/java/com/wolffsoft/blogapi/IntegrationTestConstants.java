package com.wolffsoft.blogapi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class IntegrationTestConstants {

    public static final String REGISTER_URI = "/api/auth/register";
    public static final String LOGIN_URI = "/api/auth/login";
    public static final String REFRESH_URI = "/api/auth/refresh";
    public static final String BLOGPOST_URI = "/api/posts";
    public static final String BLOGPOST_BY_ID_URI = BLOGPOST_URI + "/{id}";
    public static final String EMAIL = "nvt@nvt.nl";
    public static final String NAME = "name";
    public static final String PASSWORD_CORRECT = "password-of-minimal-12";
    public static final String AUTHORISATION_HEADER = "Authorization";
    public static final String AUTHENTICATION_SCHEME_BEARER = "Bearer %s";
    public static final String BLOGPOST_TITLE = "title";
    public static final String BLOGPOST_SUBTITLE = "subtitle";
    public static final String BLOGPOST_BODY = "body";
    public static final String BLOGPOST_IMG_URL = "imgUrl";

}
