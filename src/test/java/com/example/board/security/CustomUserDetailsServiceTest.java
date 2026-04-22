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

    @Autowired CustomUserDetailsService userDetailsService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void loadUserByUsername_존재하는_사용자_UserDetails를_반환한다() {
        String hashed = passwordEncoder.encode("pw12345");
        userRepository.save(new User("alice", hashed));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_미존재_사용자_UsernameNotFoundException을_던진다() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void 반환된_UserDetails_password는_저장된_해시와_같다() {
        String hashed = passwordEncoder.encode("pw12345");
        userRepository.save(new User("bob", hashed));

        UserDetails details = userDetailsService.loadUserByUsername("bob");

        assertThat(details.getPassword()).isEqualTo(hashed);
        assertThat(details.getPassword()).startsWith("$2");
    }
}
