package com.example.board.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.board.user.ApiKeyRepository;
import com.example.board.user.ApiKeyService;
import com.example.board.user.User;
import com.example.board.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApiKeyAuthenticationFilterTest {

  @Autowired ApiKeyAuthenticationFilter filter;
  @Autowired ApiKeyService apiKeyService;
  @Autowired UserRepository userRepository;
  @Autowired ApiKeyRepository apiKeyRepository;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
    SecurityContextHolder.clearContext();
    userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void X_API_Key_헤더가_없으면_SecurityContext는_비어있고_체인은_그대로_통과한다() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isSameAs(req);
  }

  @Test
  void X_API_Key가_정상이면_SecurityContext에_키_소유자_username이_세팅된다() throws Exception {
    String plain = apiKeyService.issue("alice", "k").plainKey();
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader("X-API-Key", plain);
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getName()).isEqualTo("alice");
    assertThat(auth.isAuthenticated()).isTrue();
    assertThat(chain.getRequest()).isSameAs(req);
  }

  @Test
  void X_API_Key가_잘못되면_SecurityContext는_비어있고_필터는_401을_직접_쓰지_않는다() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader("X-API-Key", "bk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(res.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isSameAs(req);
  }

  @Test
  void 이미_인증된_컨텍스트가_있으면_API_key가_정상이어도_덮어쓰지_않는다() throws Exception {
    String plain = apiKeyService.issue("alice", "k").plainKey();
    Authentication preset = new UsernamePasswordAuthenticationToken("preset-user", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(preset);

    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader("X-API-Key", plain);
    MockHttpServletResponse res = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(req, res, chain);

    Authentication after = SecurityContextHolder.getContext().getAuthentication();
    assertThat(after).isNotNull();
    assertThat(after.getName()).isEqualTo("preset-user");
  }
}
