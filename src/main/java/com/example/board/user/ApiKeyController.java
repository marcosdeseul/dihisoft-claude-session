package com.example.board.user;

import com.example.board.user.ApiKeyResponses.ApiKeySummaryResponse;
import com.example.board.user.ApiKeyResponses.IssuedApiKeyResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/api-keys")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  public ApiKeyController(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public IssuedApiKeyResponse issue(
      @RequestBody IssueRequest request, Authentication authentication) {
    return IssuedApiKeyResponse.from(
        apiKeyService.issue(authentication.getName(), request.label()));
  }

  @GetMapping
  public List<ApiKeySummaryResponse> list(Authentication authentication) {
    return apiKeyService.listFor(authentication.getName()).stream()
        .map(ApiKeySummaryResponse::from)
        .toList();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable Long id, Authentication authentication) {
    apiKeyService.revoke(authentication.getName(), id);
  }

  public record IssueRequest(String label) {}
}
