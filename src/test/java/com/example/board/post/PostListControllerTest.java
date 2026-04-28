package com.example.board.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.user.User;
import com.example.board.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostListControllerTest {

  @Autowired MockMvc mockMvc;

  @Autowired PostRepository postRepository;

  @Autowired UserRepository userRepository;

  @Autowired PasswordEncoder passwordEncoder;

  User alice;

  @BeforeEach
  void setUp() {
    postRepository.deleteAll();
    userRepository.deleteAll();
    alice = userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
  }

  @AfterEach
  void tearDown() {
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void GET_목록은_인증없이_200이며_content는_배열이다() throws Exception {
    mockMvc
        .perform(get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10));
  }

  @Test
  void 빈_목록이면_content는_빈배열이고_totalElements_0_totalPages_0이다() throws Exception {
    mockMvc
        .perform(get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));
  }

  @Test
  void 여러_글을_저장하면_최신순으로_정렬된다() throws Exception {
    postRepository.save(new Post("old", "first", alice));
    Thread.sleep(10);
    postRepository.save(new Post("mid", "second", alice));
    Thread.sleep(10);
    Post latest = postRepository.save(new Post("new", "third", alice));

    mockMvc
        .perform(get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.content[0].id").value(latest.getId()))
        .andExpect(jsonPath("$.content[0].title").value("new"));
  }

  @Test
  void 파라미터_없이_호출하면_page0_size10으로_동작한다() throws Exception {
    for (int i = 0; i < 15; i++) {
      postRepository.save(new Post("t" + i, "c" + i, alice));
    }

    mockMvc
        .perform(get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content.length()").value(10));
  }

  @Test
  void page1_size5로_조회하면_두번째_페이지가_5건_반환된다() throws Exception {
    for (int i = 0; i < 15; i++) {
      postRepository.save(new Post("t" + i, "c" + i, alice));
    }

    mockMvc
        .perform(get("/api/posts").param("page", "1").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.content.length()").value(5));
  }

  @Test
  void 페이지_범위_밖이면_빈_content와_올바른_totalPages를_반환한다() throws Exception {
    for (int i = 0; i < 3; i++) {
      postRepository.save(new Post("t" + i, "c" + i, alice));
    }

    mockMvc
        .perform(get("/api/posts").param("page", "5").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void size가_0이면_400을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/posts").param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void size가_음수면_400을_반환한다() throws Exception {
    mockMvc.perform(get("/api/posts").param("size", "-1")).andExpect(status().isBadRequest());
  }

  @Test
  void page가_음수면_400을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/posts").param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void 목록_응답_항목은_id_title_content_authorUsername_createdAt_updatedAt을_포함한다() throws Exception {
    postRepository.save(new Post("hello", "world", alice));

    mockMvc
        .perform(get("/api/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").isNumber())
        .andExpect(jsonPath("$.content[0].title").value("hello"))
        .andExpect(jsonPath("$.content[0].content").value("world"))
        .andExpect(jsonPath("$.content[0].authorUsername").value("alice"))
        .andExpect(jsonPath("$.content[0].createdAt").exists())
        .andExpect(jsonPath("$.content[0].updatedAt").exists());
  }
}
