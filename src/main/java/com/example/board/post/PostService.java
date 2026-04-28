package com.example.board.post;

import com.example.board.user.User;
import com.example.board.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PostService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;

  public PostService(PostRepository postRepository, UserRepository userRepository) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public Page<Post> list(Pageable pageable) {
    return postRepository.findAllByOrderByCreatedAtDesc(pageable);
  }

  @Transactional(readOnly = true)
  public Post get(Long id) {
    return postRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: " + id));
  }

  public Post create(String title, String content, String username) {
    requireText(title, "title");
    requireText(content, "content");
    User author =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + username));
    return postRepository.save(new Post(title, content, author));
  }

  public Post update(Long id, String title, String content, String username) {
    requireText(title, "title");
    requireText(content, "content");
    Post post = get(id);
    assertOwnedBy(post, username);
    post.update(title, content);
    return post;
  }

  private void requireText(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + "은 필수입니다");
    }
  }

  public void delete(Long id, String username) {
    Post post = get(id);
    assertOwnedBy(post, username);
    postRepository.delete(post);
  }

  private void assertOwnedBy(Post post, String username) {
    if (!post.isOwnedBy(username)) {
      throw new AccessDeniedException("본인의 글만 수정/삭제할 수 있습니다");
    }
  }
}
