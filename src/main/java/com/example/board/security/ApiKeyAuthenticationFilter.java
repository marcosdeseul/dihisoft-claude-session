package com.example.board.security;

import com.example.board.user.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  static final String HEADER = "X-API-Key";

  private final ApiKeyService apiKeyService;

  public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);
    if (header == null
        || header.isEmpty()
        || SecurityContextHolder.getContext().getAuthentication() != null) {
      chain.doFilter(request, response);
      return;
    }
    Optional<String> username = apiKeyService.authenticate(header);
    if (username.isPresent()) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(username.get(), null, Collections.emptyList());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    chain.doFilter(request, response);
  }
}
