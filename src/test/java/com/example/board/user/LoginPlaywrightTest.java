package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.BeforeEach;
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

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        authService.signup(new SignupRequest("playwright_user", "pw12345playwright"));
    }

    @Test
    void 브라우저로_로그인_폼을_제출하면_성공_메시지가_표시된다() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(true));
            try {
                Page page = browser.newPage();
                page.navigate("http://localhost:" + port + "/login");

                page.fill("#username", "playwright_user");
                page.fill("#password", "pw12345playwright");
                page.click("button[type=submit]");

                page.waitForURL("**/login?success=true");

                assertThat(page.url()).contains("success=true");
                assertThat(page.content()).contains("로그인 완료");
            } finally {
                browser.close();
            }
        }
    }
}
