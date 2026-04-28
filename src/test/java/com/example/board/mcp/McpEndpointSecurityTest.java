package com.example.board.mcp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.post.PostRepository;
import com.example.board.user.ApiKeyRepository;
import com.example.board.user.ApiKeyService;
import com.example.board.user.User;
import com.example.board.user.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class McpEndpointSecurityTest {

  @Autowired MockMvc mockMvc;
  @Autowired ApiKeyService apiKeyService;
  @Autowired ApiKeyRepository apiKeyRepository;
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired PasswordEncoder passwordEncoder;

  private static final String TOOLS_LIST_BODY =
      "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";

  String aliceKey;

  @BeforeEach
  void setUp() {
    postRepository.deleteAll();
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
    userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
    aliceKey = apiKeyService.issue("alice", "mcp-key").plainKey();
  }

  @AfterEach
  void tearDown() {
    postRepository.deleteAll();
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void POST_mcp_헤더_없으면_401() throws Exception {
    mockMvc
        .perform(
            post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(TOOLS_LIST_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void POST_mcp_잘못된_X_API_Key면_401() throws Exception {
    mockMvc
        .perform(
            post("/mcp")
                .header("X-API-Key", "bk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(TOOLS_LIST_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void POST_mcp_revoked된_키면_401() throws Exception {
    long keyId =
        userRepository
            .findByUsername("alice")
            .map(u -> apiKeyRepository.findByUserOrderByCreatedAtDesc(u).get(0).getId())
            .orElseThrow();
    apiKeyService.revoke("alice", keyId);

    mockMvc
        .perform(
            post("/mcp")
                .header("X-API-Key", aliceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(TOOLS_LIST_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void POST_mcp_정상키_tools_list면_5개_도구가_노출된다() throws Exception {
    mockMvc
        .perform(
            post("/mcp")
                .header("X-API-Key", aliceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(TOOLS_LIST_BODY))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    Matchers.allOf(
                        Matchers.containsString("posts_list"),
                        Matchers.containsString("posts_get"),
                        Matchers.containsString("posts_create"),
                        Matchers.containsString("posts_update"),
                        Matchers.containsString("posts_delete"))));
  }

  @Test
  void POST_mcp_정상키_tools_call_posts_create면_DB에_저장되고_authorUsername이_키소유자() throws Exception {
    String body =
        "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"posts_create\","
            + "\"arguments\":{\"title\":\"via mcp\",\"content\":\"hello\"}}}";

    mockMvc
        .perform(
            post("/mcp")
                .header("X-API-Key", aliceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("alice")));
  }

  @Test
  void POST_mcp_Accept_헤더에_event_stream_없으면_400() throws Exception {
    mockMvc
        .perform(
            post("/mcp")
                .header("X-API-Key", aliceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(TOOLS_LIST_BODY))
        .andExpect(status().isBadRequest());
  }
}
