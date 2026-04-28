package com.example.board.post;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public record PostResponse(
    Long id,
    String title,
    String content,
    String authorUsername,
    Instant createdAt,
    Instant updatedAt) {

  public static PostResponse from(Post post) {
    return new PostResponse(
        post.getId(),
        post.getTitle(),
        post.getContent(),
        post.getAuthor().getUsername(),
        post.getCreatedAt(),
        post.getUpdatedAt());
  }

  public record PageResponse<T>(
      List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
      return new PageResponse<>(
          page.getContent(),
          page.getNumber(),
          page.getSize(),
          page.getTotalElements(),
          page.getTotalPages());
    }
  }
}
