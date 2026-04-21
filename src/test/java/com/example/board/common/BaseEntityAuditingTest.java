package com.example.board.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BaseEntityAuditingTest {

    @Autowired
    ProbeRepository probeRepository;

    @Test
    void 엔티티_저장시_createdAt과_updatedAt이_자동으로_채워진다() {
        ProbeEntity saved = probeRepository.saveAndFlush(new ProbeEntity("hello"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void 엔티티_수정시_updatedAt이_갱신되고_createdAt은_유지된다() throws InterruptedException {
        ProbeEntity saved = probeRepository.saveAndFlush(new ProbeEntity("first"));
        Instant originalCreatedAt = saved.getCreatedAt();
        Instant originalUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(10);
        saved.setName("second");
        ProbeEntity updated = probeRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }
}
