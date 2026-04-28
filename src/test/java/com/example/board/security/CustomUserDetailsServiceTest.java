package com.example.board.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.board.user.User;
import com.example.board.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CustomUserDetailsServiceTest {

  @Autowired CustomUserDetailsService customUserDetailsService;

  @Autowired UserRepository userRepository;

  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void cleanDb() {
    userRepository.deleteAll();
  }

  @Test
  void loadUserByUsername_존재하는_사용자면_저장된_username과_해시된_password를_담은_UserDetails를_반환한다() {
    String hashed = passwordEncoder.encode("pw12345");
    userRepository.save(new User("uds_alice", hashed));

    UserDetails details = customUserDetailsService.loadUserByUsername("uds_alice");

    assertThat(details.getUsername()).isEqualTo("uds_alice");
    assertThat(details.getPassword()).isEqualTo(hashed);
    assertThat(details.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsername_미존재_username이면_UsernameNotFoundException이_발생한다() {
    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("uds_nobody"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("uds_nobody");
  }
}
