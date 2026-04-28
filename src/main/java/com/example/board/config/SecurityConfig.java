package com.example.board.config;

import com.example.board.security.ApiKeyAuthenticationFilter;
import com.example.board.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
public class SecurityConfig {

  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  public SecurityConfig(
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"))
                    .permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**"))
                    .permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/signup"))
                    .permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/login"))
                    .permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/css/**"))
                    .permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**"))
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**")
                    .permitAll()
                    .requestMatchers(
                        AntPathRequestMatcher.antMatcher("/mcp"),
                        AntPathRequestMatcher.antMatcher("/mcp/**"))
                    .authenticated()
                    .requestMatchers(
                        new RegexRequestMatcher(
                            "^(?!/api(?:/|$))(?!/h2-console(?:/|$))(?!/mcp(?:/|$))[^.]*$",
                            HttpMethod.GET.name()))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class);
    return http.build();
  }
}
