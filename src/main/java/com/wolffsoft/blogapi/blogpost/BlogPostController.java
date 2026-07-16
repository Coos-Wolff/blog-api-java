package com.wolffsoft.blogapi.blogpost;

import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse;
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest;
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse;
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;

    @PostMapping
    public ResponseEntity<BlogPostResponse> create(@Valid @RequestBody CreateBlogPostRequest createBlogPostRequest) {
        BlogPostResponse response = blogPostService.create(createBlogPostRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostResponse> getById(@PathVariable UUID id) {
        BlogPostResponse response = blogPostService.getById(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<BlogPostResponse>> findAll(Pageable pageable) {
        PagedResponse<BlogPostResponse> response = blogPostService.findAll(pageable);
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BlogPostResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateBlogPostRequest request) {
        BlogPostResponse updated = blogPostService.update(id, request);
        return ResponseEntity.ok().body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        blogPostService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
