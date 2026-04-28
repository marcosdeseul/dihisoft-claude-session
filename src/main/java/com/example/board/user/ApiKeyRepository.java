package com.example.board.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  Optional<ApiKey> findByPrefix(String prefix);

  boolean existsByPrefix(String prefix);

  List<ApiKey> findByUserOrderByCreatedAtDesc(User user);

  Optional<ApiKey> findByIdAndUser(Long id, User user);
}
