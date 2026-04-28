package com.example.board.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthViewController {

  private final AuthService authService;

  public AuthViewController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/login")
  public String loginForm(Model model) {
    if (!model.containsAttribute("loginRequest")) {
      model.addAttribute("loginRequest", new LoginRequest());
    }
    return "login";
  }

  @PostMapping("/login")
  public String loginSubmit(
      @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
      BindingResult bindingResult,
      HttpServletResponse response) {
    if (bindingResult.hasErrors()) {
      return "login";
    }
    try {
      TokenResponse token = authService.login(loginRequest);
      Cookie cookie = new Cookie("accessToken", token.accessToken());
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setMaxAge((int) token.expiresIn());
      response.addCookie(cookie);
      return "redirect:/login?success=true";
    } catch (AuthenticationException ex) {
      bindingResult.reject("login.invalid", AuthErrorMessages.INVALID_CREDENTIALS);
      return "login";
    }
  }
}
