package com.example.board.user;

import com.example.board.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtTokenProvider jwtTokenProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Transactional
  public User signup(SignupRequest request) {
    String username = request.getUsername() == null ? "" : request.getUsername().trim();
    if (username.isEmpty()) {
      throw new IllegalArgumentException("username은 필수입니다");
    }
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("이미 사용 중인 username입니다: " + username);
    }
    String hashed = passwordEncoder.encode(request.getPassword());
    return userRepository.save(new User(username, hashed));
  }

  public TokenResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
    String accessToken = jwtTokenProvider.generateToken(authentication.getName());
    long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;
    return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
  }
}
