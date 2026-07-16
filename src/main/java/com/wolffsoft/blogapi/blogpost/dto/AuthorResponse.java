package com.wolffsoft.blogapi.blogpost.dto;

import java.util.UUID;

public record AuthorResponse(
        UUID id,
        String name
) {
}
