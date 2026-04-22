package com.example.board.user;

import com.example.board.common.ErrorResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {

    static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/signup")
    @ResponseBody
    public ResponseEntity<SignupResponse> signupApi(@Valid @RequestBody SignupRequest request) {
        User user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignupResponse(user.getId(), user.getUsername()));
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> loginApi(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            AuthService.LoginResult result = authService.login(request);
            return ResponseEntity.ok(
                    new LoginResponse(result.token(), result.username(), result.expiresIn()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                            "자격 증명이 올바르지 않습니다", httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        if (!model.containsAttribute("signupRequest")) {
            model.addAttribute("signupRequest", new SignupRequest());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(
            @Valid @ModelAttribute("signupRequest") SignupRequest signupRequest,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }
        try {
            authService.signup(signupRequest);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("signup.duplicate", ex.getMessage());
            return "signup";
        }
        return "redirect:/signup?success=true";
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
            AuthService.LoginResult result = authService.login(loginRequest);
            Cookie cookie = new Cookie(AUTH_COOKIE_NAME, result.token());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) (result.expiresIn() / 1000));
            response.addCookie(cookie);
        } catch (AuthenticationException ex) {
            bindingResult.reject("login.failed", "자격 증명이 올바르지 않습니다");
            return "login";
        }
        return "redirect:/login?success=true";
    }

    public record SignupResponse(Long id, String username) {
    }

    public record LoginResponse(String token, String username, long expiresIn) {
    }
}
