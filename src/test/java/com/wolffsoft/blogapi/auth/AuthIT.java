package com.wolffsoft.blogapi.auth;

import com.wolffsoft.blogapi.IntegrationTestBase;
import com.wolffsoft.blogapi.auth.dto.LoginRequest;
import com.wolffsoft.blogapi.auth.dto.RefreshRequest;
import com.wolffsoft.blogapi.auth.dto.RegisterRequest;
import com.wolffsoft.blogapi.auth.dto.TokenResponse;
import com.wolffsoft.blogapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.wolffsoft.blogapi.IntegrationTestConstants.EMAIL;
import static com.wolffsoft.blogapi.IntegrationTestConstants.LOGIN_URI;
import static com.wolffsoft.blogapi.IntegrationTestConstants.NAME;
import static com.wolffsoft.blogapi.IntegrationTestConstants.PASSWORD_CORRECT;
import static com.wolffsoft.blogapi.IntegrationTestConstants.REFRESH_URI;
import static com.wolffsoft.blogapi.IntegrationTestConstants.REGISTER_URI;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthIT extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should test register successful")
    void testRegisterSuccessful() {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT);

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    @DisplayName("Should test duplicate email on register")
    void testDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT);

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should test too short password on register")
    void testTooShortPassword() {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "password");

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    @DisplayName("Should test no name on register")
    void testNoName() {
        RegisterRequest request = new RegisterRequest(EMAIL, null, PASSWORD_CORRECT);

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    @DisplayName("Should test wrong email format on register")
    void testWrongEmailFormat() {
        RegisterRequest request = new RegisterRequest("email-wrong-format.nl", NAME, PASSWORD_CORRECT);

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    @DisplayName("Should test login success")
    void testLoginSuccessful() {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT);
        LoginRequest loginRequest = new LoginRequest(EMAIL, PASSWORD_CORRECT);

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();

        restTestClient.post()
                .uri(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    @DisplayName("Should test wrong password on login")
    void testWrongPassword() {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT);
        LoginRequest loginRequest = new LoginRequest(EMAIL, "Some-other-password");

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();

        restTestClient.post()
                .uri(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();

    }

    @Test
    @DisplayName("Should test refresh success")
    void testRefreshSuccess() {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT);
        LoginRequest loginRequest = new LoginRequest(EMAIL, PASSWORD_CORRECT);

        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();

        TokenResponse tokenResponseFromLogin = restTestClient.post()
                .uri(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();

        RefreshRequest refreshRequest = new RefreshRequest(tokenResponseFromLogin.refreshToken());

        TokenResponse tokenResponseFromRefresh = restTestClient.post()
                .uri(REFRESH_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(refreshRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(tokenResponseFromRefresh.accessToken()).isNotNull();
        assertThat(tokenResponseFromRefresh.refreshToken()).isNotNull();
        assertThat(tokenResponseFromLogin.refreshToken()).isEqualTo(tokenResponseFromRefresh.refreshToken());
    }

    @Test
    @DisplayName("Should test unauthorized on refresh with garbage token")
    void testUnauthorizedOnRefreshWithGarbageToken() {
        RefreshRequest refreshRequest = new RefreshRequest("garbage.token.now");

        restTestClient.post()
                .uri(REFRESH_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(refreshRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
