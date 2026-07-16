package com.wolffsoft.blogapi.blogpost;

import com.wolffsoft.blogapi.auth.CurrentUserProvider;
import com.wolffsoft.blogapi.blogpost.dto.AuthorResponse;
import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse;
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest;
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse;
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest;
import com.wolffsoft.blogapi.blogpost.exception.BlogPostNotFoundException;
import com.wolffsoft.blogapi.exception.ForbiddenException;
import com.wolffsoft.blogapi.user.User;
import com.wolffsoft.blogapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogPostService {

    private static final String BLOG_POST_NOT_FOUND_MESSAGE = "Not blog post found for id: [%s]";

    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public BlogPostResponse create(CreateBlogPostRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.getReferenceById(userId);
        BlogPost blogPost = createBlogPostFromRequest(request, user);
        BlogPost saved = blogPostRepository.save(blogPost);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getById(UUID id) {
        BlogPost blogPost =  blogPostRepository.findById(id)
                .orElseThrow(() ->
                        new BlogPostNotFoundException(String.format(BLOG_POST_NOT_FOUND_MESSAGE, id)));
        return toResponse(blogPost);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlogPostResponse> findAll(Pageable pageable) {
        Page<BlogPostResponse> page =  blogPostRepository.findAll(pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Transactional
    public BlogPostResponse update(UUID postId, UpdateBlogPostRequest request) {
        BlogPost toUpdate = findByIdOrThrow(postId);
        assertCanModify(toUpdate);
        BlogPost updated = updateBlogPost(toUpdate, request);
        return toResponse(updated);
    }

    @Transactional
    public void delete(UUID postId) {
        BlogPost toDelete = findByIdOrThrow(postId);
        assertCanModify(toDelete);
        blogPostRepository.delete(toDelete);
    }

    private BlogPost createBlogPostFromRequest(CreateBlogPostRequest request, User author) {
        BlogPost blogPost = new BlogPost();
        blogPost.setTitle(request.title());
        blogPost.setSubtitle(request.subtitle());
        blogPost.setDate(LocalDate.now());
        blogPost.setBody(request.body());
        blogPost.setImgUrl(request.imgUrl());
        blogPost.setAuthor(author);
        return blogPost;
    }

    private BlogPostResponse toResponse(BlogPost blogPost) {
        AuthorResponse authorResponse = toAuthorResponse(blogPost.getAuthor().getId(), blogPost.getAuthor().getName());
        return new BlogPostResponse(
                blogPost.getId(),
                blogPost.getTitle(),
                blogPost.getSubtitle(),
                blogPost.getDate(),
                blogPost.getBody(),
                blogPost.getImgUrl(),
                authorResponse
        );
    }

    private AuthorResponse toAuthorResponse(UUID id, String name) {
        return new AuthorResponse(id, name);
    }

    private BlogPost findByIdOrThrow(UUID id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new BlogPostNotFoundException(String.format(BLOG_POST_NOT_FOUND_MESSAGE, id)));
    }

    private void assertCanModify(BlogPost blogPost) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        boolean isAuthor = blogPost.getAuthor().getId().equals(currentUserId);
        boolean isAdmin = currentUserProvider.isCurrentUserAdmin();
        if (!isAuthor && !isAdmin) {
            throw new ForbiddenException("Not allowed to modify blog post");
        }
    }

    private BlogPost updateBlogPost(BlogPost blogPost, UpdateBlogPostRequest request) {
        if (request.title() != null) {
            blogPost.setTitle(request.title());
        }
        if (request.subtitle() != null) {
            blogPost.setSubtitle(request.subtitle());
        }
        if (request.body() != null) {
            blogPost.setBody(request.body());
        }
        if (request.imgUrl() != null) {
            blogPost.setImgUrl(request.imgUrl());
        }
        return blogPost;
    }
}
