package com.healthline.backend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Two near-simultaneous enrollOrReenroll calls for the same idNumber can both pass the
 * "not found" check before either commits its insert (enrollOrReenroll isn't atomic —
 * each repository call is its own transaction). The loser must adopt the winner's
 * identity, not crash. A real race is timing-dependent and unsuitable for a deterministic
 * test, so this uses a mocked repository to force the exact interleaving instead.
 */
class EmbeddingStoreRaceTest {

  @Test
  void enrollOrReenrollAdoptsTheWinnersIdentityWhenAConcurrentInsertWinsTheRace() {
    IdentityEmbeddingRepository repository = mock(IdentityEmbeddingRepository.class);
    EmbeddingStore store = new EmbeddingStore(repository);
    IdentityEmbedding winner = new IdentityEmbedding("winner-id", "0000000000000", List.of(0.1));

    when(repository.findByIdNumber("0000000000000"))
        .thenReturn(Optional.empty()) // our own check: not found yet
        .thenReturn(Optional.of(winner)); // retry after conflict: the other request's row
    when(repository.save(any(IdentityEmbedding.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    String identityId = store.enrollOrReenroll("0000000000000", List.of(0.1));

    assertThat(identityId).isEqualTo("winner-id");
  }
}
