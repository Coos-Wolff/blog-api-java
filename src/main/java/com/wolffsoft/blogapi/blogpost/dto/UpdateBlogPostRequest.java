package com.wolffsoft.blogapi.blogpost.dto;

import jakarta.validation.constraints.Size;

public record UpdateBlogPostRequest(
        @Size(max = 250)
        String title,

        @Size(max = 250)
        String subtitle,

        String body,

        @Size(max = 250)
        String imgUrl
) {
}
