package com.example.board.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaFallbackTest {

  @Autowired MockMvc mockMvc;

  @Test
  void GET_루트는_index_html로_forward한다() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void GET_매칭되지않은_경로는_index_html로_forward한다() throws Exception {
    mockMvc
        .perform(get("/random-route"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void GET_중첩_경로도_index_html로_forward한다() throws Exception {
    mockMvc
        .perform(get("/board/123"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void GET_api_경로는_폴백되지않고_404를_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/auth/nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(result -> assertThat(result.getResponse().getForwardedUrl()).isNull());
  }

  @Test
  void GET_점이포함된_정적자원은_폴백되지않는다() throws Exception {
    mockMvc
        .perform(get("/favicon.ico"))
        .andExpect(result -> assertThat(result.getResponse().getForwardedUrl()).isNull());

    mockMvc
        .perform(get("/assets/main.js"))
        .andExpect(status().isNotFound())
        .andExpect(result -> assertThat(result.getResponse().getForwardedUrl()).isNull());
  }

  @Test
  void POST_매칭되지않은_경로는_폴백되지않는다() throws Exception {
    mockMvc
        .perform(post("/random-route"))
        .andExpect(result -> assertThat(result.getResponse().getForwardedUrl()).isNull());
  }

  @Test
  void GET_h2_console_경로는_폴백되지않는다() throws Exception {
    mockMvc
        .perform(get("/h2-console/"))
        .andExpect(status().is(not(401)))
        .andExpect(status().is(not(403)))
        .andExpect(result -> assertThat(result.getResponse().getForwardedUrl()).isNull());
  }

  @Test
  void GET_signup은_React_SPA로_forward된다() throws Exception {
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }
}
