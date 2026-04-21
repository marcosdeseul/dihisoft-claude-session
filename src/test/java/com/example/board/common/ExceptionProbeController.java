package com.example.board.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/__probe")
@Profile("test")
class ExceptionProbeController {

    @PostMapping("/validation")
    void validation(@Valid @RequestBody ProbeRequest request) {
    }

    @GetMapping("/runtime")
    String runtime() {
        throw new RuntimeException("boom-do-not-leak");
    }

    @GetMapping("/access-denied")
    String accessDenied() {
        throw new AccessDeniedException("nope");
    }

    @GetMapping("/illegal")
    String illegal() {
        throw new IllegalArgumentException("bad argument");
    }

    @GetMapping("/secured")
    String secured() {
        return "ok";
    }

    record ProbeRequest(@NotBlank String name, @Min(1) int age) {
    }
}
