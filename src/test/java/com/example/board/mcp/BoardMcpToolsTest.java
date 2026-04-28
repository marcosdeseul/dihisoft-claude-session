package com.example.board.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.board.post.Post;
import com.example.board.post.PostRepository;
import com.example.board.post.PostResponse;
import com.example.board.user.User;
import com.example.board.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class BoardMcpToolsTest {

  @Autowired BoardMcpTools tools;
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired PasswordEncoder passwordEncoder;

  User alice;
  User bob;

  @BeforeEach
  void setUp() {
    postRepository.deleteAll();
    userRepository.deleteAll();
    SecurityContextHolder.clearContext();
    alice = userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
    bob = userRepository.save(new User("bob", passwordEncoder.encode("pw12345")));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  private void authAs(String username) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
  }

  @Nested
  class 조회 {

    @Test
    void posts_list_정상_페이지응답() {
      postRepository.save(new Post("t1", "c1", alice));
      postRepository.save(new Post("t2", "c2", bob));

      PostResponse.PageResponse<PostResponse> page = tools.posts_list(0, 10);

      assertThat(page.totalElements()).isEqualTo(2);
      assertThat(page.content()).hasSize(2);
    }

    @Test
    void posts_get_정상_단건() {
      Post saved = postRepository.save(new Post("hello", "world", alice));

      PostResponse res = tools.posts_get(saved.getId());

      assertThat(res.id()).isEqualTo(saved.getId());
      assertThat(res.authorUsername()).isEqualTo("alice");
    }

    @Test
    void posts_list_page음수면_400() {
      assertThatThrownBy(() -> tools.posts_list(-1, 10))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void posts_list_size0이면_400() {
      assertThatThrownBy(() -> tools.posts_list(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void posts_get_없는_id면_404() {
      assertThatThrownBy(() -> tools.posts_get(9999L))
          .isInstanceOf(ResponseStatusException.class)
          .satisfies(
              e ->
                  assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }
  }

  @Nested
  class 생성 {

    @Test
    void posts_create_정상_DB저장되고_authorUsername이_컨텍스트와_일치() {
      authAs("alice");

      PostResponse res = tools.posts_create("via mcp", "hello");

      assertThat(res.id()).isNotNull();
      assertThat(res.title()).isEqualTo("via mcp");
      assertThat(res.authorUsername()).isEqualTo("alice");
      assertThat(postRepository.findById(res.id())).isPresent();
    }

    @Test
    void posts_create_빈_title이면_400() {
      authAs("alice");

      assertThatThrownBy(() -> tools.posts_create("", "본문"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void posts_create_빈_content면_400() {
      authAs("alice");

      assertThatThrownBy(() -> tools.posts_create("제목", ""))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void posts_create_SecurityContext가_비어있으면_명시적_예외() {
      assertThatThrownBy(() -> tools.posts_create("제목", "본문"))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  class 수정 {

    @Test
    void posts_update_본인글이면_변경이_반영된다() {
      authAs("alice");
      Post saved = postRepository.save(new Post("old", "old-body", alice));

      PostResponse res = tools.posts_update(saved.getId(), "new", "new-body");

      assertThat(res.title()).isEqualTo("new");
      assertThat(res.content()).isEqualTo("new-body");
    }

    @Test
    void posts_update_타인글이면_AccessDeniedException() {
      authAs("bob");
      Post saved = postRepository.save(new Post("alice-글", "본문", alice));

      assertThatThrownBy(() -> tools.posts_update(saved.getId(), "x", "y"))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void posts_update_없는_id면_404() {
      authAs("alice");

      assertThatThrownBy(() -> tools.posts_update(9999L, "x", "y"))
          .isInstanceOf(ResponseStatusException.class);
    }
  }

  @Nested
  class 삭제 {

    @Test
    void posts_delete_본인글이면_삭제되고_이후_조회는_404() {
      authAs("alice");
      Post saved = postRepository.save(new Post("t", "c", alice));

      tools.posts_delete(saved.getId());

      assertThat(postRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void posts_delete_타인글이면_AccessDeniedException() {
      authAs("bob");
      Post saved = postRepository.save(new Post("t", "c", alice));

      assertThatThrownBy(() -> tools.posts_delete(saved.getId()))
          .isInstanceOf(AccessDeniedException.class);
    }
  }
}
