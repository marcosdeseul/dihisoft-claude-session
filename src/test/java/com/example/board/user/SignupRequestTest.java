package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SignupRequestTest {

  @Test
  void 인자_생성자로_필드가_세팅된다() {
    SignupRequest request = new SignupRequest("alice", "pw12345");

    assertThat(request.getUsername()).isEqualTo("alice");
    assertThat(request.getPassword()).isEqualTo("pw12345");
  }

  @Test
  void 기본_생성자_후_setter로_필드가_갱신된다() {
    SignupRequest request = new SignupRequest();
    request.setUsername("bob");
    request.setPassword("pw99999");

    assertThat(request.getUsername()).isEqualTo("bob");
    assertThat(request.getPassword()).isEqualTo("pw99999");
  }
}
