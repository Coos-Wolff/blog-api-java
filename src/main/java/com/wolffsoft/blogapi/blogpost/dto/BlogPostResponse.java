package com.wolffsoft.blogapi.blogpost.dto;

import com.wolffsoft.blogapi.blogpost.BlogPost;

import java.time.LocalDate;
import java.util.UUID;

public record BlogPostResponse(
        UUID id,
        String title,
        String subtitle,
        LocalDate date,
        String body,
        String imgUrl,
        AuthorResponse author
) {
}
