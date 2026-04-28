package com.example.board.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 요청한_경로가_없으면_404_JSON을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/definitely-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.path").value("/api/definitely-not-exist"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void 검증_실패시_400_JSON을_반환하고_필드명을_포함한다() throws Exception {
    mockMvc
        .perform(
            post("/api/__probe/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"age\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value(containsString("name")))
        .andExpect(jsonPath("$.path").value("/api/__probe/validation"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void 런타임예외는_500_JSON을_반환하고_스택트레이스를_숨긴다() throws Exception {
    mockMvc
        .perform(get("/api/__probe/runtime"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.message").value(not(containsString("boom-do-not-leak"))))
        .andExpect(jsonPath("$.message").value(not(containsString("RuntimeException"))))
        .andExpect(jsonPath("$.path").value("/api/__probe/runtime"));
  }

  @Test
  void 접근_거부는_403_JSON을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/__probe/access-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.path").value("/api/__probe/access-denied"));
  }

  @Test
  void IllegalArgument는_400_JSON을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/__probe/illegal"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value(containsString("bad argument")));
  }
}
