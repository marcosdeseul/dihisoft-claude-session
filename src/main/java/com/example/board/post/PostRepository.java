package com.example.board.post;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

  @EntityGraph(attributePaths = "author")
  Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "author")
  Optional<Post> findById(Long id);
}
