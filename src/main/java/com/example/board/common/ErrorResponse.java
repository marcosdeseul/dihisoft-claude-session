package com.example.board.common;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
    int status, String error, String message, String path, Instant timestamp) {

  public static ErrorResponse of(HttpStatus status, String message, String path) {
    return new ErrorResponse(
        status.value(), status.getReasonPhrase(), message, path, Instant.now());
  }
}
