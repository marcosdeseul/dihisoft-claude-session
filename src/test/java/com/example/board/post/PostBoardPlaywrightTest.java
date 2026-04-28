package com.example.board.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.board.user.AuthService;
import com.example.board.user.SignupRequest;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("playwright")
class PostBoardPlaywrightTest {

  @LocalServerPort int port;

  @Autowired AuthService authService;

  @Test
  void 브라우저로_로그인_후_글_작성_수정_삭제까지_한_흐름이_동작한다() {
    String username = "pwb" + (System.currentTimeMillis() % 1_000_000_000L);
    String password = "pw12345playwright";
    authService.signup(new SignupRequest(username, password));

    String original = "Playwright 작성글 " + System.nanoTime();
    String edited = original + " (수정됨)";

    try (Playwright playwright = Playwright.create()) {
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      try {
        Page page = browser.newPage();

        // 1) 로그인
        page.navigate("http://localhost:" + port + "/login");
        page.waitForSelector(
            "#username", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.fill("#username", username);
        page.fill("#password", password);
        page.click("button[type=submit]");

        // 2) 목록 진입 — "게시판" 헤딩 가시
        page.waitForSelector(
            "text=게시판",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(page.url()).doesNotContain("/login");

        // 3) 글쓰기
        page.click("text=글쓰기");
        page.waitForSelector(
            "#title", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.fill("#title", original);
        page.fill("#content", "Playwright 본문\n첫 글");
        page.click("button[type=submit]");

        // 4) 상세에서 작성 글 가시
        page.waitForSelector(
            "text=" + original,
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(page.url()).matches(".*/posts/\\d+$");

        // 5) 수정
        page.click("text=수정");
        page.waitForSelector(
            "#title", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.fill("#title", edited);
        page.click("button[type=submit]");
        page.waitForSelector(
            "text=" + edited,
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // 6) 삭제 — confirm 자동 수락
        page.onceDialog(dialog -> dialog.accept());
        page.click("text=삭제");
        page.waitForSelector(
            "text=게시판",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(page.url()).endsWith("/");
        assertThat(page.content()).doesNotContain(edited);
      } finally {
        browser.close();
      }
    }
  }
}
