package com.healthline.backend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmbeddingStoreTest {

  @Autowired private EmbeddingStore embeddingStore;
  @Autowired private IdentityEmbeddingRepository repository;

  private List<Double> vector(double seed) {
    return DoubleStream.iterate(seed, d -> d + 0.001).limit(512).boxed().toList();
  }

  @Test
  void findReturnsEmptyWhenNoEmbeddingStoredForIdentity() {
    Optional<List<Double>> result = embeddingStore.find("identity-1");

    assertThat(result).isEmpty();
  }

  @Test
  void enrollOrReenrollThenFindReturnsTheSameEmbedding() {
    List<Double> embedding = vector(0.1);

    String identityId = embeddingStore.enrollOrReenroll("0000000000000", embedding);

    assertThat(embeddingStore.find(identityId)).contains(embedding);
  }

  @Test
  void reEnrollingTheSameIdNumberReusesTheIdentityIdAndOverwritesRatherThanDuplicating() {
    String firstIdentityId = embeddingStore.enrollOrReenroll("0000000000000", vector(0.1));
    String secondIdentityId = embeddingStore.enrollOrReenroll("0000000000000", vector(0.2));

    assertThat(secondIdentityId).isEqualTo(firstIdentityId);
    assertThat(repository.findAll()).hasSize(1);
    assertThat(embeddingStore.find(firstIdentityId)).contains(vector(0.2));
  }

  @Test
  void embeddingsForDifferentIdNumbersAreIsolated() {
    String identityId1 = embeddingStore.enrollOrReenroll("0000000000000", vector(0.1));
    String identityId2 = embeddingStore.enrollOrReenroll("1111111111111", vector(0.9));

    assertThat(embeddingStore.find(identityId1)).contains(vector(0.1));
    assertThat(embeddingStore.find(identityId2)).contains(vector(0.9));
  }

  @Test
  void a512DimensionVectorRoundTripsWithoutPrecisionLoss() {
    List<Double> embedding = vector(0.123456789);

    String identityId = embeddingStore.enrollOrReenroll("0000000000000", embedding);

    assertThat(embeddingStore.find(identityId)).contains(embedding);
  }

  @Test
  void findIdentityIdByIdNumberReturnsEmptyWhenNeverEnrolled() {
    assertThat(embeddingStore.findIdentityIdByIdNumber("0000000000000")).isEmpty();
  }

  @Test
  void findIdentityIdByIdNumberReturnsTheIdentityIdItWasEnrolledUnder() {
    String identityId = embeddingStore.enrollOrReenroll("0000000000000", vector(0.1));

    assertThat(embeddingStore.findIdentityIdByIdNumber("0000000000000")).contains(identityId);
  }
}
