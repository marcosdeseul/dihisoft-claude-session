package com.example.board.user;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

  @NotBlank(message = "username은 필수입니다")
  private String username;

  @NotBlank(message = "password는 필수입니다")
  private String password;

  public LoginRequest() {}

  public LoginRequest(String username, String password) {
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
