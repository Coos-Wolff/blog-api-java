package com.wolffsoft.blogapi.blogpost;

import com.wolffsoft.blogapi.auth.CurrentUserProvider;
import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse;
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest;
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse;
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest;
import com.wolffsoft.blogapi.blogpost.exception.BlogPostNotFoundException;
import com.wolffsoft.blogapi.exception.ForbiddenException;
import com.wolffsoft.blogapi.user.User;
import com.wolffsoft.blogapi.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceTest {

    @Mock
    private BlogPostRepository blogPostRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private BlogPost post;
    @Mock
    private User author;

    @InjectMocks
    private BlogPostService blogPostService;

    @Test
    @DisplayName("create resolves the author from the current user id, " +
            "builds a BlogPost from the request fields with today's date, saves it, and returns the mapped response")
    void createValidRequestSavesBlogPostWithRequestFieldsAndTodayDateAndReturnsMappedResponse() {
        // given
        UUID currentUserId = UUID.randomUUID();
        CreateBlogPostRequest request = new CreateBlogPostRequest(
                "Title", "Subtitle", "Body text", "http://img.example.com/a.png");
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.getReferenceById(currentUserId)).thenReturn(author);
        when(author.getId()).thenReturn(currentUserId);
        when(author.getName()).thenReturn("Author Name");
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        BlogPostResponse response = blogPostService.create(request);

        // then
        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(blogPostRepository).save(captor.capture());
        BlogPost saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo(request.title());
        assertThat(saved.getSubtitle()).isEqualTo(request.subtitle());
        assertThat(saved.getBody()).isEqualTo(request.body());
        assertThat(saved.getImgUrl()).isEqualTo(request.imgUrl());
        assertThat(saved.getDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getAuthor()).isSameAs(author);

        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.subtitle()).isEqualTo(request.subtitle());
        assertThat(response.body()).isEqualTo(request.body());
        assertThat(response.imgUrl()).isEqualTo(request.imgUrl());
        assertThat(response.date()).isEqualTo(LocalDate.now());
        assertThat(response.author().id()).isEqualTo(currentUserId);
        assertThat(response.author().name()).isEqualTo("Author Name");
    }

    @Test
    @DisplayName("getById returns a BlogPostResponse mapped from the found blog post, " +
            "including nested author id and name")
    void getByIdFoundReturnsMappedResponse() {
        // given
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(blogPostRepository.findById(id)).thenReturn(Optional.of(post));
        when(post.getId()).thenReturn(id);
        when(post.getTitle()).thenReturn("Title");
        when(post.getSubtitle()).thenReturn("Subtitle");
        when(post.getDate()).thenReturn(LocalDate.now());
        when(post.getBody()).thenReturn("Body");
        when(post.getImgUrl()).thenReturn("http://img.example.com/a.png");
        when(post.getAuthor()).thenReturn(author);
        when(author.getId()).thenReturn(authorId);
        when(author.getName()).thenReturn("Author Name");

        // when
        BlogPostResponse response = blogPostService.getById(id);

        // then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.subtitle()).isEqualTo("Subtitle");
        assertThat(response.date()).isEqualTo(LocalDate.now());
        assertThat(response.body()).isEqualTo("Body");
        assertThat(response.imgUrl()).isEqualTo("http://img.example.com/a.png");
        assertThat(response.author().id()).isEqualTo(authorId);
        assertThat(response.author().name()).isEqualTo("Author Name");
    }

    @Test
    @DisplayName("getById throws BlogPostNotFoundException when no blog post exists for the given id")
    void getByIdNotFoundThrowsBlogPostNotFoundException() {
        // given
        UUID id = UUID.randomUUID();
        when(blogPostRepository.findById(id)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> blogPostService.getById(id))
                .isInstanceOf(BlogPostNotFoundException.class);
    }

    @Test
    @DisplayName("findAll returns a PagedResponse with mapped content and pagination metadata matching the underlying Page")
    void findAllReturnsPagedResponseWithMappedContentAndPaginationMetadata() {
        // given
        Pageable pageable = PageRequest.of(0, 2);
        UUID authorId = UUID.randomUUID();
        when(post.getId()).thenReturn(UUID.randomUUID());
        when(post.getTitle()).thenReturn("Title");
        when(post.getSubtitle()).thenReturn("Subtitle");
        when(post.getDate()).thenReturn(LocalDate.now());
        when(post.getBody()).thenReturn("Body");
        when(post.getImgUrl()).thenReturn("http://img.example.com/a.png");
        when(post.getAuthor()).thenReturn(author);
        when(author.getId()).thenReturn(authorId);
        when(author.getName()).thenReturn("Author Name");
        Page<BlogPost> page = new PageImpl<>(List.of(post), pageable, 5);
        when(blogPostRepository.findAll(pageable)).thenReturn(page);

        // when
        PagedResponse<BlogPostResponse> response = blogPostService.findAll(pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Title");
        assertThat(response.content().get(0).author().id()).isEqualTo(authorId);
        assertThat(response.page()).isEqualTo(page.getNumber());
        assertThat(response.size()).isEqualTo(page.getSize());
        assertThat(response.totalElements()).isEqualTo(page.getTotalElements());
        assertThat(response.totalPages()).isEqualTo(page.getTotalPages());
        assertThat(response.hasNext()).isEqualTo(page.hasNext());
        assertThat(response.hasPrevious()).isEqualTo(page.hasPrevious());
    }

    @Test
    @DisplayName("update applies only the non-null title field and leaves subtitle, body, and imgUrl unchanged")
    void updatePartialRequestChangesOnlyNonNullFieldsAndLeavesRestUnchanged() {
        // given
        UUID postId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        BlogPost existingPost = new BlogPost();
        existingPost.setTitle("Original Title");
        existingPost.setSubtitle("Original Subtitle");
        existingPost.setBody("Original Body");
        existingPost.setImgUrl("http://original.example.com/img.png");
        existingPost.setAuthor(author);
        UpdateBlogPostRequest request = new UpdateBlogPostRequest("New Title", null, null, null);
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(author.getId()).thenReturn(currentUserId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);

        // when
        blogPostService.update(postId, request);

        // then
        assertThat(existingPost.getTitle()).isEqualTo("New Title");
        assertThat(existingPost.getSubtitle()).isEqualTo("Original Subtitle");
        assertThat(existingPost.getBody()).isEqualTo("Original Body");
        assertThat(existingPost.getImgUrl()).isEqualTo("http://original.example.com/img.png");
    }

    @Test
    @DisplayName("update applies all non-null fields when the request specifies multiple fields")
    void updateMultiFieldRequestAppliesAllNonNullFields() {
        // given
        UUID postId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        BlogPost existingPost = new BlogPost();
        existingPost.setTitle("Original Title");
        existingPost.setSubtitle("Original Subtitle");
        existingPost.setBody("Original Body");
        existingPost.setImgUrl("http://original.example.com/img.png");
        existingPost.setAuthor(author);
        UpdateBlogPostRequest request = new UpdateBlogPostRequest(
                "Updated Title", "Updated Subtitle", "Updated Body", "http://updated.example.com/img.png");
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(author.getId()).thenReturn(currentUserId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);

        // when
        blogPostService.update(postId, request);

        // then
        assertThat(existingPost.getTitle()).isEqualTo("Updated Title");
        assertThat(existingPost.getSubtitle()).isEqualTo("Updated Subtitle");
        assertThat(existingPost.getBody()).isEqualTo("Updated Body");
        assertThat(existingPost.getImgUrl()).isEqualTo("http://updated.example.com/img.png");
    }

    @Test
    @DisplayName("update succeeds when the current user is the blog post's author")
    void updateCurrentUserIsAuthorSucceeds() {
        // given
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        BlogPost existingPost = new BlogPost();
        existingPost.setTitle("Original Title");
        existingPost.setSubtitle("Original Subtitle");
        existingPost.setBody("Original Body");
        existingPost.setImgUrl("http://original.example.com/img.png");
        existingPost.setAuthor(author);
        UpdateBlogPostRequest request = new UpdateBlogPostRequest("Updated Title", null, null, null);
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(author.getId()).thenReturn(authorId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(authorId);

        // when
        blogPostService.update(postId, request);

        // then
        assertThat(existingPost.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("update succeeds when the current user is not the author but is an admin")
    void updateCurrentUserIsAdminNotAuthorSucceeds() {
        // given
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        BlogPost existingPost = new BlogPost();
        existingPost.setTitle("Original Title");
        existingPost.setSubtitle("Original Subtitle");
        existingPost.setBody("Original Body");
        existingPost.setImgUrl("http://original.example.com/img.png");
        existingPost.setAuthor(author);
        UpdateBlogPostRequest request = new UpdateBlogPostRequest("Updated Title", null, null, null);
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(author.getId()).thenReturn(authorId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
        when(currentUserProvider.isCurrentUserAdmin()).thenReturn(true);

        // when
        blogPostService.update(postId, request);

        // then
        assertThat(existingPost.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("update throws ForbiddenException and leaves the post unmodified when the current user is neither the author nor an admin")
    void updateCurrentUserNeitherAuthorNorAdminThrowsForbiddenExceptionAndDoesNotModifyPost() {
        // given
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        BlogPost existingPost = new BlogPost();
        existingPost.setTitle("Original Title");
        existingPost.setSubtitle("Original Subtitle");
        existingPost.setBody("Original Body");
        existingPost.setImgUrl("http://original.example.com/img.png");
        existingPost.setAuthor(author);
        UpdateBlogPostRequest request = new UpdateBlogPostRequest("Updated Title", null, null, null);
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(author.getId()).thenReturn(authorId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
        when(currentUserProvider.isCurrentUserAdmin()).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> blogPostService.update(postId, request))
                .isInstanceOf(ForbiddenException.class);
        assertThat(existingPost.getTitle()).isEqualTo("Original Title");
        assertThat(existingPost.getSubtitle()).isEqualTo("Original Subtitle");
        assertThat(existingPost.getBody()).isEqualTo("Original Body");
        assertThat(existingPost.getImgUrl()).isEqualTo("http://original.example.com/img.png");
    }

    @Test
    @DisplayName("update throws BlogPostNotFoundException when no blog post exists for the given id")
    void updateNotFoundThrowsBlogPostNotFoundException() {
        // given
        UUID postId = UUID.randomUUID();
        UpdateBlogPostRequest request = new UpdateBlogPostRequest("Updated Title", null, null, null);
        when(blogPostRepository.findById(postId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> blogPostService.update(postId, request))
                .isInstanceOf(BlogPostNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes the blog post when the current user is its author")
    void deleteCurrentUserIsAuthorDeletesBlogPost() {
        // given
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getAuthor()).thenReturn(author);
        when(author.getId()).thenReturn(authorId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(authorId);

        // when
        blogPostService.delete(postId);

        // then
        verify(blogPostRepository).delete(post);
    }

    @Test
    @DisplayName("delete removes the blog post when the current user is not the author but is an admin")
    void deleteCurrentUserIsAdminNotAuthorDeletesBlogPost() {
        // given
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getAuthor()).thenReturn(author);
        when(author.getId()).thenReturn(authorId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
        when(currentUserProvider.isCurrentUserAdmin()).thenReturn(true);

        // when
        blogPostService.delete(postId);

        // then
        verify(blogPostRepository).delete(post);
    }

    @Test
    @DisplayName("delete throws ForbiddenException and does not delete when the current user is neither the author nor an admin")
    void deleteCurrentUserNeitherAuthorNorAdminThrowsForbiddenExceptionAndDoesNotDelete() {
        // given
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(post.getAuthor()).thenReturn(author);
        when(author.getId()).thenReturn(authorId);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
        when(currentUserProvider.isCurrentUserAdmin()).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> blogPostService.delete(postId))
                .isInstanceOf(ForbiddenException.class);
        verify(blogPostRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete throws BlogPostNotFoundException when no blog post exists for the given id")
    void deleteNotFoundThrowsBlogPostNotFoundException() {
        // given
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> blogPostService.delete(postId))
                .isInstanceOf(BlogPostNotFoundException.class);
    }
}
