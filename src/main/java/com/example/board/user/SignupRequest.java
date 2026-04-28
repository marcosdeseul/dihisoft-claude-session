package com.example.board.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {

  @NotBlank(message = "username은 필수입니다")
  @Size(min = 3, max = 20, message = "username은 3~20자여야 합니다")
  private String username;

  @NotBlank(message = "password는 필수입니다")
  @Size(min = 6, message = "password는 최소 6자여야 합니다")
  private String password;

  public SignupRequest() {}

  public SignupRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
