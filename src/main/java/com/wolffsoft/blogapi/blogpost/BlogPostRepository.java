package com.wolffsoft.blogapi.blogpost;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    @Override
    @EntityGraph(attributePaths = "author")
    Page<BlogPost> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "author")
    Optional<BlogPost> findById(UUID authorId);
}
