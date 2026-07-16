package com.wolffsoft.blogapi.blogpost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBlogPostRequest(
        @NotBlank
        @Size(max = 250)
        String title,

        @NotBlank
        @Size(max = 250)
        String subtitle,

        @NotBlank
        String body,

        @NotBlank
        @Size(max = 250)
        String imgUrl
) {
}
