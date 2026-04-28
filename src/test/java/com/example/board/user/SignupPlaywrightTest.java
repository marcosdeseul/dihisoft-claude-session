package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("playwright")
class SignupPlaywrightTest {

  @LocalServerPort int port;

  @Test
  void 브라우저로_회원가입_폼을_제출하면_성공_메시지가_표시된다() {
    String username = "pw" + (System.currentTimeMillis() % 1_000_000_000L);
    try (Playwright playwright = Playwright.create()) {
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      try {
        Page page = browser.newPage();
        page.navigate("http://localhost:" + port + "/signup");

        page.waitForSelector(
            "#username", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.fill("#username", username);
        page.fill("#password", "pw12345playwright");
        page.click("button[type=submit]");

        page.waitForSelector(
            "text=가입 완료", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        assertThat(page.content()).contains("가입 완료");
      } finally {
        browser.close();
      }
    }
  }
}
