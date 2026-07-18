package com.healthline.backend.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Owns the one-embedding-per-identity and one-identity-per-idNumber invariants. */
@Component
public class EmbeddingStore {

  private final IdentityEmbeddingRepository repository;

  EmbeddingStore(IdentityEmbeddingRepository repository) {
    this.repository = repository;
  }

  public Optional<List<Double>> find(String identityId) {
    return repository.findByIdentityId(identityId).map(IdentityEmbedding::getEmbedding);
  }

  public Optional<String> findIdentityIdByIdNumber(String idNumber) {
    return repository.findByIdNumber(idNumber).map(IdentityEmbedding::getIdentityId);
  }

  /**
   * Finds the identity already enrolled under this idNumber and updates its embedding, or
   * creates a fresh one — a single idNumber lookup either way, rather than a separate
   * "decide identityId" query followed by another "fetch for update" query.
   */
  public String enrollOrReenroll(String idNumber, List<Double> embedding) {
    return repository
        .findByIdNumber(idNumber)
        .map(
            existing -> {
              // The entity above and this save() are two separate repository calls, each
              // its own transaction — existing is detached by the time we get here, so the
              // mutation needs an explicit save() to persist (dirty-checking only works
              // within one still-open Hibernate session, which this isn't).
              existing.updateEmbedding(embedding);
              repository.save(existing);
              return existing.getIdentityId();
            })
        .orElseGet(
            () -> {
              String identityId = UUID.randomUUID().toString();
              repository.save(new IdentityEmbedding(identityId, idNumber, embedding));
              return identityId;
            });
  }
}
