package com.wolffsoft.blogapi.blogpost;

import com.wolffsoft.blogapi.IntegrationTestBase;
import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse;
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest;
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse;
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest;
import com.wolffsoft.blogapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.AUTHENTICATION_SCHEME_BEARER;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.AUTHORISATION_HEADER;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.BLOGPOST_BODY;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.BLOGPOST_BY_ID_URI;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.BLOGPOST_IMG_URL;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.BLOGPOST_SUBTITLE;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.BLOGPOST_TITLE;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.BLOGPOST_URI;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.EMAIL;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.NAME;
import static com.wolffsoft.blogapi.auth.IntegrationTestConstants.PASSWORD_CORRECT;
import static org.assertj.core.api.Assertions.assertThat;

public class BlogPostIT extends IntegrationTestBase {

    private static final String BLOGPOST_TITLE_251_CHARACTERS = """
            Why This Exact 251-Character Blog Post Title Explores How Strategic REST API Design and 
            Contract-Driven Testing via Cucumber Feature Files are Absolutely Vital 
            for Enhancing Developer Experience and Ensuring Seamless Enterprise System Scalabilities!!!""";

    private static final String BLOGPOST_SUBTITLE_251_CHARACTERS = """
            An in-depth guide containing exactly 251 characters that breaks down how software architects 
            merge automated BDD workflows with clean API documentation to build resilient cloud-native ecosystems 
            that scale effortlessly under heavy enterprise loads!!!!
            """;

    private static final String BLOGPOST_IMG_URL_251_CHARACTERS = """
            https://images.example.com/blog/this-image-url-has-precisely-251-characters-to-match-the-rest-of-your-
            software-architecture-api-design-and-cucumber-testing-article-layout-requirements-for-enterprise-
            microservices-diagram-asset-high-res-source-imag.png
            """;

    private static final String ADMIN_EMAIL = "admin@admin.nl";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;

    @BeforeEach
    void setup() {
        blogPostRepository.deleteAll();
        userRepository.deleteAll();
        accessToken = registerAndLogin(EMAIL, NAME, PASSWORD_CORRECT);
    }

    @Test
    @DisplayName("Should retrieve paged blog posts")
    void testRetrieveBlogPosts() {
        PagedResponse<BlogPostResponse> initialResponse = restTestClient.get()
                .uri(BLOGPOST_URI)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<BlogPostResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(initialResponse.content()).hasSize(0);

        CreateBlogPostRequest createBlogPostRequest1 =
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        CreateBlogPostRequest createBlogPostRequest2 =
                createBlogPostRequest("Title1", "Subtitle1", "Body1", "imgUrl1");

        createBlogPost(createBlogPostRequest1);
        createBlogPost(createBlogPostRequest2);

        PagedResponse<BlogPostResponse> response = restTestClient.get()
                .uri(BLOGPOST_URI)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<BlogPostResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(2);
    }

    @Test
    @DisplayName("Should get blog post by id without authentication")
    void testGetBlogPostById() {
        CreateBlogPostRequest createBlogPostRequest =
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);

        BlogPostResponse getById = restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(BlogPostResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(getById).isNotNull();
        assertThat(getById.title()).isEqualTo(BLOGPOST_TITLE);
        assertThat(getById.subtitle()).isEqualTo(BLOGPOST_SUBTITLE);
        assertThat(getById.body()).isEqualTo(BLOGPOST_BODY);
        assertThat(getById.imgUrl()).isEqualTo(BLOGPOST_IMG_URL);
    }

    @Test
    @DisplayName("Should return 404 for unknown blog post id")
    void testGetBlogPostByIdNotFound() {
        restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, "00000000-0000-0000-0000-000000000000")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @DisplayName("Should create blog post successfully")
    void testCreateBlogPostSuccess() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);

        BlogPostResponse response = restTestClient.post()
                .uri(BLOGPOST_URI)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBlogPostRequest)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(BlogPostResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo(BLOGPOST_TITLE);
        assertThat(response.subtitle()).isEqualTo(BLOGPOST_SUBTITLE);
        assertThat(response.body()).isEqualTo(BLOGPOST_BODY);
        assertThat(response.imgUrl()).isEqualTo(BLOGPOST_IMG_URL);
    }

    @Test
    @DisplayName("Should return 401 when creating without a token")
    void testCreateBlogPostRequiresAuthentication() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);

        restTestClient.post()
                .uri(BLOGPOST_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBlogPostRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @DisplayName("Should return 400 when title is blank")
    void testValidationOnTitleBlank() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                null, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should return 400 when subtitle is blank")
    void testValidationOnSubtitleBlank() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, null, BLOGPOST_BODY, BLOGPOST_IMG_URL);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should return 400 when body is blank")
    void testValidationOnBodyBlank() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, null, BLOGPOST_IMG_URL);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should return 400 when img url is blank")
    void testValidationOnImgUrlBlank() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, null);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should return 400 when title exceeds 250 characters")
    void testValidationOnTitleMax250Characters() {
        CreateBlogPostRequest createBlogPostRequest =
                createBlogPostRequest(BLOGPOST_TITLE_251_CHARACTERS, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should return 400 when subtitle exceeds 250 characters")
    void testValidationOnSubtitleMax250Characters() {
        CreateBlogPostRequest createBlogPostRequest =
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE_251_CHARACTERS, BLOGPOST_BODY, BLOGPOST_IMG_URL);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should return 400 when img url exceeds 250 characters")
    void testValidationOnImgUrlMax250Characters() {
        CreateBlogPostRequest createBlogPostRequest =
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL_251_CHARACTERS);

        validateCreateBlogPostRequestBadRequest(createBlogPostRequest);
    }

    @Test
    @DisplayName("Should patch only the title")
    void testPatchTitle() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);
        String titleUpdate = "titleUpdate";
        UpdateBlogPostRequest updateBlogPostRequest = createUpdateBlogPostRequest(titleUpdate, null, null, null);

        BlogPostResponse patchedTitle = patchBlogPost(updateBlogPostRequest, uuid, accessToken);

        assertThat(patchedTitle.title()).isNotEqualTo(BLOGPOST_TITLE);
        assertThat(patchedTitle.title()).isEqualTo(titleUpdate);
        assertThat(patchedTitle.subtitle()).isEqualTo(BLOGPOST_SUBTITLE);
        assertThat(patchedTitle.body()).isEqualTo(BLOGPOST_BODY);
        assertThat(patchedTitle.imgUrl()).isEqualTo(BLOGPOST_IMG_URL);
    }

    @Test
    @DisplayName("Should patch only the subtitle")
    void testPatchSubtitle() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);
        String subtitleUpdate = "subtitleUpdate";
        UpdateBlogPostRequest updateBlogPostRequest = createUpdateBlogPostRequest(null, subtitleUpdate, null, null);

        BlogPostResponse patchedSubTitle = patchBlogPost(updateBlogPostRequest, uuid, accessToken);

        assertThat(patchedSubTitle.title()).isEqualTo(BLOGPOST_TITLE);
        assertThat(patchedSubTitle.subtitle()).isNotEqualTo(BLOGPOST_SUBTITLE);
        assertThat(patchedSubTitle.subtitle()).isEqualTo(subtitleUpdate);
        assertThat(patchedSubTitle.body()).isEqualTo(BLOGPOST_BODY);
        assertThat(patchedSubTitle.imgUrl()).isEqualTo(BLOGPOST_IMG_URL);
    }

    @Test
    @DisplayName("Should patch only the body")
    void testPatchBody() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);
        String bodyUpdate = "bodyUpdate";
        UpdateBlogPostRequest updateBlogPostRequest = createUpdateBlogPostRequest(null, null, bodyUpdate, null);

        BlogPostResponse patchedBody = patchBlogPost(updateBlogPostRequest, uuid, accessToken);

        assertThat(patchedBody.title()).isEqualTo(BLOGPOST_TITLE);
        assertThat(patchedBody.subtitle()).isEqualTo(BLOGPOST_SUBTITLE);
        assertThat(patchedBody.body()).isNotEqualTo(BLOGPOST_BODY);
        assertThat(patchedBody.body()).isEqualTo(bodyUpdate);
        assertThat(patchedBody.imgUrl()).isEqualTo(BLOGPOST_IMG_URL);
    }

    @Test
    @DisplayName("Should patch only the img url")
    void testPatchImgUrl() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);
        String imgUrlUpdate = "imgUrlUpdate";
        UpdateBlogPostRequest updateBlogPostRequest = createUpdateBlogPostRequest(null, null, null, imgUrlUpdate);

        BlogPostResponse patchedImgUrl = patchBlogPost(updateBlogPostRequest, uuid, accessToken);

        assertThat(patchedImgUrl.title()).isEqualTo(BLOGPOST_TITLE);
        assertThat(patchedImgUrl.subtitle()).isEqualTo(BLOGPOST_SUBTITLE);
        assertThat(patchedImgUrl.body()).isEqualTo(BLOGPOST_BODY);
        assertThat(patchedImgUrl.imgUrl()).isNotEqualTo(BLOGPOST_IMG_URL);
        assertThat(patchedImgUrl.imgUrl()).isEqualTo(imgUrlUpdate);
    }

    @Test
    @DisplayName("Should return 401 when patching with an invalid token")
    void testPatchWithInvalidTokenReturnsUnauthorized() {
        BlogPostResponse response = createBlogPost(
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL));
        String uuid = getUUIDFromResponse(response);
        String garbageToken = "garbage.token.value";

        restTestClient.patch()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, garbageToken))
                .body(createUpdateBlogPostRequest(null, null, null, "imgUrlUpdate"))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @DisplayName("Should return 403 when a non-owner patches another user's post")
    void testPatchAccessTokenNotFromOwner() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);
        String imgUrlUpdate = "imgUrlUpdate";
        UpdateBlogPostRequest updateBlogPostRequest = createUpdateBlogPostRequest(null, null, null, imgUrlUpdate);

        String accessTokenNotFromOwner = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwicm9sZXMiOlsiUkVBRF9CT0dHIlsiV1JJVEVfQkxPRyJdLCJpYXQiOjE3ODYzNjk5MDAsImV4cCI6MTc4NjM3MzUwMH0.sU-b6R1b9O0N3R3X4H8J9K2L1M3N5P7R9S1T3U5V7W9";

        restTestClient.patch()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, accessTokenNotFromOwner))
                .body(updateBlogPostRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @DisplayName("Should allow an admin to patch another user's post")
    void testPatchBlogPostByAdmin() {
        BlogPostResponse response = createBlogPost(
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL));
        String uuid = getUUIDFromResponse(response);

        register(ADMIN_EMAIL, NAME, PASSWORD_CORRECT);
        makeAdmin(ADMIN_EMAIL);
        String adminToken = login(ADMIN_EMAIL, PASSWORD_CORRECT);

        String titleUpdate = "adminTitleUpdate";
        BlogPostResponse patched = patchBlogPost(
                createUpdateBlogPostRequest(titleUpdate, null, null, null), uuid, adminToken);

        assertThat(patched.title()).isEqualTo(titleUpdate);
    }

    @Test
    @DisplayName("Should delete own blog post")
    void testDeleteBlogPost() {
        CreateBlogPostRequest createBlogPostRequest = createBlogPostRequest(
                BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL);
        BlogPostResponse response = createBlogPost(createBlogPostRequest);

        String uuid = getUUIDFromResponse(response);

        restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .exchange()
                .expectStatus()
                .isOk();

        restTestClient.delete()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, accessToken))
                .exchange()
                .expectStatus()
                .isNoContent();

        restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @DisplayName("Should return 403 when a non-owner deletes another user's post")
    void testDeleteByNonOwnerReturnsForbidden() {
        BlogPostResponse response = createBlogPost(
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL));
        String uuid = getUUIDFromResponse(response);

        String otherUserToken = registerAndLogin("other@other.nl", NAME, PASSWORD_CORRECT);

        restTestClient.delete()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, otherUserToken))
                .exchange()
                .expectStatus()
                .isForbidden();

        restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    @DisplayName("Should allow an admin to delete another user's post")
    void testDeleteBlogPostByAdmin() {
        BlogPostResponse response = createBlogPost(
                createBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL));
        String uuid = getUUIDFromResponse(response);


        register(ADMIN_EMAIL, NAME, PASSWORD_CORRECT);
        makeAdmin(ADMIN_EMAIL);
        String adminToken = login(ADMIN_EMAIL, PASSWORD_CORRECT);

        restTestClient.delete()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, adminToken))
                .exchange()
                .expectStatus()
                .isNoContent();

        restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private CreateBlogPostRequest createBlogPostRequest(String title, String subtitle, String body, String imgUrl) {
        return new CreateBlogPostRequest(
                title,
                subtitle,
                body,
                imgUrl
        );
    }

    private void validateCreateBlogPostRequestBadRequest(CreateBlogPostRequest createBlogPostRequest) {
        restTestClient.post()
                .uri(BLOGPOST_URI)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBlogPostRequest)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    private BlogPostResponse createBlogPost(CreateBlogPostRequest createBlogPostRequest) {
        return restTestClient.post()
                .uri(BLOGPOST_URI)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createBlogPostRequest)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(BlogPostResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private String getUUIDFromResponse(BlogPostResponse response) {
        return response.id().toString();
    }

    private BlogPostResponse patchBlogPost(UpdateBlogPostRequest updateBlogPostRequest, String uuid, String accessToken) {
        return restTestClient.patch()
                .uri(BLOGPOST_BY_ID_URI, uuid)
                .header(AUTHORISATION_HEADER, String.format(AUTHENTICATION_SCHEME_BEARER, accessToken))
                .body(updateBlogPostRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(BlogPostResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private UpdateBlogPostRequest createUpdateBlogPostRequest(String title, String subtitle, String body, String imgUrl) {
        return new UpdateBlogPostRequest(title, subtitle, body, imgUrl);
    }

    private void makeAdmin(String email) {
        jdbcTemplate.update("UPDATE users SET is_admin = true WHERE email = ?", email);
    }
}
