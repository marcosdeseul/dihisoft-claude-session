package com.example.board;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BoardApplicationContextLoadsTest {

  @Test
  void 스프링_컨텍스트가_정상적으로_로드된다() {}
}
