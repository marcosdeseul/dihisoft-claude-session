package com.example.board.mcp;

import com.example.board.post.PostResponse;
import com.example.board.post.PostService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class BoardMcpTools {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 10;

  private final PostService postService;

  public BoardMcpTools(PostService postService) {
    this.postService = postService;
  }

  @McpTool(name = "posts_list", description = "게시글 목록을 페이지로 조회합니다. page는 0-기반.")
  public PostResponse.PageResponse<PostResponse> posts_list(
      @McpToolParam(description = "0-based page number (기본 0)") Integer page,
      @McpToolParam(description = "page size (기본 10)") Integer size) {
    int p = page == null ? DEFAULT_PAGE : page;
    int s = size == null ? DEFAULT_SIZE : size;
    if (p < 0) {
      throw new IllegalArgumentException("page는 0 이상이어야 합니다");
    }
    if (s <= 0) {
      throw new IllegalArgumentException("size는 1 이상이어야 합니다");
    }
    return PostResponse.PageResponse.from(
        postService.list(PageRequest.of(p, s)).map(PostResponse::from));
  }

  @McpTool(name = "posts_get", description = "게시글 단건을 ID로 조회합니다.")
  public PostResponse posts_get(@McpToolParam(description = "post id", required = true) Long id) {
    return PostResponse.from(postService.get(id));
  }

  @McpTool(name = "posts_create", description = "현재 인증된 사용자(API key 소유자) 명의로 게시글을 작성합니다.")
  public PostResponse posts_create(
      @McpToolParam(description = "글 제목", required = true) String title,
      @McpToolParam(description = "본문", required = true) String content) {
    return PostResponse.from(postService.create(title, content, currentUsername()));
  }

  @McpTool(name = "posts_update", description = "본인이 작성한 게시글의 제목/본문을 수정합니다.")
  public PostResponse posts_update(
      @McpToolParam(description = "post id", required = true) Long id,
      @McpToolParam(description = "새 제목", required = true) String title,
      @McpToolParam(description = "새 본문", required = true) String content) {
    return PostResponse.from(postService.update(id, title, content, currentUsername()));
  }

  @McpTool(name = "posts_delete", description = "본인이 작성한 게시글을 삭제합니다.")
  public String posts_delete(@McpToolParam(description = "post id", required = true) Long id) {
    postService.delete(id, currentUsername());
    return "deleted: " + id;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new IllegalStateException("인증된 사용자만 호출할 수 있습니다");
    }
    return auth.getName();
  }
}
