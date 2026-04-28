package com.example.board.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void 핸들러가_호출되면_403과_ErrorResponse_JSON을_기록한다() throws Exception {
    RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/some");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("거부"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).contains("application/json");
    assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

    String body = response.getContentAsString();
    assertThat(body).contains("\"status\":403");
    assertThat(body).contains("\"error\":\"Forbidden\"");
    assertThat(body).contains("\"message\":\"접근이 거부되었습니다\"");
    assertThat(body).contains("\"path\":\"/api/some\"");
    assertThat(body).contains("\"timestamp\"");
  }
}
