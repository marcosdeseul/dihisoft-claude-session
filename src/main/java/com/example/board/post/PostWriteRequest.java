package com.example.board.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PostWriteRequest {

  private PostWriteRequest() {}

  public record Create(
      @NotBlank(message = "title은 필수입니다") @Size(max = 100, message = "title은 최대 100자입니다")
          String title,
      @NotBlank(message = "content는 필수입니다") String content) {}

  public record Update(
      @NotBlank(message = "title은 필수입니다") @Size(max = 100, message = "title은 최대 100자입니다")
          String title,
      @NotBlank(message = "content는 필수입니다") String content) {}
}
