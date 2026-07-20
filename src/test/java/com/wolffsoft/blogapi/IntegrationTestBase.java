package com.wolffsoft.blogapi;

import com.wolffsoft.blogapi.auth.dto.LoginRequest;
import com.wolffsoft.blogapi.auth.dto.RegisterRequest;
import com.wolffsoft.blogapi.auth.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Objects;

import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.LOGIN_URI;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.REGISTER_URI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class IntegrationTestBase {


    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @Autowired
    protected RestTestClient restTestClient;
    
    protected void register(String email, String name, String password) {
        restTestClient.post()
                .uri(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, name, password))
                .exchange()
                .expectStatus()
                .isCreated();
    }

    protected String login(String email, String password) {
        return Objects.requireNonNull(restTestClient.post()
                        .uri(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new LoginRequest(email, password))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(TokenResponse.class)
                        .returnResult()
                        .getResponseBody())
                .accessToken();
    }

    protected String registerAndLogin(String email, String name, String password) {
        register(email, name, password);
        return login(email, password);
    }
}
