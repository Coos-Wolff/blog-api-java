package com.wolffsoft.blogapi.blogpost;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    Page<BlogPost> findByAuthorId(UUID authorId, Pageable pageable);
}
