package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;

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
class LoginPlaywrightTest {

  @LocalServerPort int port;

  @Autowired AuthService authService;

  @Test
  void 브라우저로_로그인_성공시_홈으로_이동한다() {
    String username = "pwl" + (System.currentTimeMillis() % 1_000_000_000L);
    String password = "pw12345playwright";
    authService.signup(new SignupRequest(username, password));

    try (Playwright playwright = Playwright.create()) {
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      try {
        Page page = browser.newPage();
        page.navigate("http://localhost:" + port + "/login");

        page.waitForSelector(
            "#username", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.fill("#username", username);
        page.fill("#password", password);
        page.click("button[type=submit]");

        page.waitForSelector(
            "text=게시판", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        assertThat(page.url()).doesNotContain("/login");
      } finally {
        browser.close();
      }
    }
  }

  @Test
  void 브라우저로_잘못된_비밀번호_로그인시_고정_오류_메시지가_표시된다() {
    String username = "pwlbad" + (System.currentTimeMillis() % 1_000_000_000L);
    authService.signup(new SignupRequest(username, "correctpassword"));

    try (Playwright playwright = Playwright.create()) {
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      try {
        Page page = browser.newPage();
        page.navigate("http://localhost:" + port + "/login");

        page.waitForSelector(
            "#username", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.fill("#username", username);
        page.fill("#password", "wrongpassword");
        page.click("button[type=submit]");

        page.waitForSelector(
            "text=username과 password가 일치하지 않습니다",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        assertThat(page.content()).contains("username과 password가 일치하지 않습니다");
        assertThat(page.url()).endsWith("/login");
      } finally {
        browser.close();
      }
    }
  }
}
