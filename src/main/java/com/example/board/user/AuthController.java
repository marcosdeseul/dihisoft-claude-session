package com.example.board.user;

import com.example.board.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── 회원가입 ──────────────────────────────────────────────────────────────

    @PostMapping("/api/auth/signup")
    @ResponseBody
    public ResponseEntity<SignupResponse> signupApi(@Valid @RequestBody SignupRequest request) {
        User user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignupResponse(user.getId(), user.getUsername()));
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

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> loginApi(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        try {
            String token = authService.login(request);
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                            "아이디 또는 비밀번호가 올바르지 않습니다",
                            servletRequest.getRequestURI()));
        }
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
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        try {
            authService.login(loginRequest);
        } catch (AuthenticationException ex) {
            bindingResult.reject("login.failed", "아이디 또는 비밀번호가 올바르지 않습니다");
            return "login";
        }
        return "redirect:/login?success=true";
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record SignupResponse(Long id, String username) {
    }

    public record TokenResponse(String token) {
    }
}
