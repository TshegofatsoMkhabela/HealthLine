package com.healthline.backend.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
   * Finds the identity already enrolled under this idNumber and updates its embedding, or creates a
   * fresh one — a single idNumber lookup either way, rather than a separate "decide identityId"
   * query followed by another "fetch for update" query.
   *
   * <p>{@code @Transactional} keeps the read and write below in one Hibernate session and one DB
   * connection — it does NOT close the race window (Postgres's default READ COMMITTED isolation
   * still lets two concurrent calls both see "not found" before either commits). The find-then-insert
   * still isn't atomic (two concurrent enrolls for the same idNumber — e.g. a client retry after a
   * slow response — can both see "not found" and both attempt to insert). What actually stops a
   * duplicate row is the DB's UNIQUE(id_number) constraint; the catch below turns the losing
   * request's constraint violation into the same successful outcome the winning request got,
   * instead of a raw 500.
   */
  @Transactional
  public String enrollOrReenroll(String idNumber, List<Double> embedding) {
    Optional<IdentityEmbedding> existing = repository.findByIdNumber(idNumber);
    if (existing.isPresent()) {
      // existing stays attached for the rest of this transaction, so dirty-checking alone
      // would flush this mutation at commit — the explicit save() below is kept anyway as
      // a clear, immediate signal of intent rather than relying on flush timing.
      IdentityEmbedding identity = existing.get();
      identity.updateEmbedding(embedding);
      repository.save(identity);
      return identity.getIdentityId();
    }

    String identityId = UUID.randomUUID().toString();
    try {
      repository.save(new IdentityEmbedding(identityId, idNumber, embedding));
      return identityId;
    } catch (DataIntegrityViolationException e) {
      return repository
          .findByIdNumber(idNumber)
          .map(IdentityEmbedding::getIdentityId)
          .orElseThrow(() -> e);
    }
  }
}
