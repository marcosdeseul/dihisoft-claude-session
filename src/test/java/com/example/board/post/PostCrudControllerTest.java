package com.example.board.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.security.JwtTokenProvider;
import com.example.board.user.User;
import com.example.board.user.UserRepository;
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
class PostCrudControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;

    User alice;
    User bob;
    String aliceToken;
    String bobToken;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();
        alice = userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
        bob = userRepository.save(new User("bob", passwordEncoder.encode("pw12345")));
        aliceToken = "Bearer " + jwtTokenProvider.generateToken("alice");
        bobToken = "Bearer " + jwtTokenProvider.generateToken("bob");
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // -------- Happy path --------

    @Test
    void 단건조회_존재하는_id면_200과_필드_일치() throws Exception {
        Post saved = postRepository.save(new Post("hello", "world", alice));

        mockMvc.perform(get("/api/posts/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("hello"))
                .andExpect(jsonPath("$.content").value("world"))
                .andExpect(jsonPath("$.authorUsername").value("alice"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void 단건조회는_인증없이_접근_가능하다() throws Exception {
        Post saved = postRepository.save(new Post("hello", "world", alice));
        mockMvc.perform(get("/api/posts/" + saved.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void 작성_인증된_사용자면_201과_authorUsername_일치() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"첫 글\",\"content\":\"본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.authorUsername").value("alice"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void 수정_본인이면_200과_변경된_필드가_반영된다() throws Exception {
        Post saved = postRepository.save(new Post("old", "old-body", alice));

        mockMvc.perform(put("/api/posts/" + saved.getId())
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"new\",\"content\":\"new-body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("new"))
                .andExpect(jsonPath("$.content").value("new-body"))
                .andExpect(jsonPath("$.authorUsername").value("alice"));

        Post reloaded = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("new");
        assertThat(reloaded.getContent()).isEqualTo("new-body");
    }

    @Test
    void 삭제_본인이면_204이고_다시_조회하면_404() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));

        mockMvc.perform(delete("/api/posts/" + saved.getId())
                        .header("Authorization", aliceToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    // -------- Input validation (400) --------

    @Test
    void 작성_title이_null이면_400() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void 작성_title이_빈문자열이면_400() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 작성_title이_101자면_400() throws Exception {
        String title = "a".repeat(101);
        String body = "{\"title\":\"" + title + "\",\"content\":\"본문\"}";
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 작성_content가_null이면_400() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("content")));
    }

    @Test
    void 작성_content가_빈문자열이면_400() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수정_title이_null이면_400() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));
        mockMvc.perform(put("/api/posts/" + saved.getId())
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수정_content가_빈문자열이면_400() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));
        mockMvc.perform(put("/api/posts/" + saved.getId())
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------- Authentication (401) --------

    @Test
    void 작성_토큰없으면_401() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 수정_토큰없으면_401() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));
        mockMvc.perform(put("/api/posts/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 삭제_토큰없으면_401() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));
        mockMvc.perform(delete("/api/posts/" + saved.getId()))
                .andExpect(status().isUnauthorized());
    }

    // -------- Ownership (403) --------

    @Test
    void 타인의_글_수정_시도면_403이고_원본이_유지된다() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));

        mockMvc.perform(put("/api/posts/" + saved.getId())
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hacked\",\"content\":\"x\"}"))
                .andExpect(status().isForbidden());

        Post reloaded = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("t");
        assertThat(reloaded.getContent()).isEqualTo("c");
    }

    @Test
    void 타인의_글_삭제_시도면_403이고_원본이_남아있다() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));

        mockMvc.perform(delete("/api/posts/" + saved.getId())
                        .header("Authorization", bobToken))
                .andExpect(status().isForbidden());

        assertThat(postRepository.findById(saved.getId())).isPresent();
    }

    // -------- Resource state (404) --------

    @Test
    void 없는_id_단건조회면_404() throws Exception {
        mockMvc.perform(get("/api/posts/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void 없는_id_수정이면_404() throws Exception {
        mockMvc.perform(put("/api/posts/9999")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 없는_id_삭제면_404() throws Exception {
        mockMvc.perform(delete("/api/posts/9999")
                        .header("Authorization", aliceToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 이미_삭제된_글_재삭제면_404() throws Exception {
        Post saved = postRepository.save(new Post("t", "c", alice));

        mockMvc.perform(delete("/api/posts/" + saved.getId())
                        .header("Authorization", aliceToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/posts/" + saved.getId())
                        .header("Authorization", aliceToken))
                .andExpect(status().isNotFound());
    }
}
