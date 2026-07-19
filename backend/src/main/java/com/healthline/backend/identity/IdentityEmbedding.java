package com.healthline.backend.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

/** One Facenet512 embedding per verified identity — see V2__create_identity_embeddings.sql. */
@Entity
@Table(name = "identity_embeddings")
class IdentityEmbedding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "identity_id", nullable = false, unique = true)
  private String identityId;

  @Column(name = "id_number", nullable = false, unique = true)
  private String idNumber;

  @Convert(converter = EmbeddingConverter.class)
  @Column(name = "embedding", nullable = false)
  private List<Double> embedding;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected IdentityEmbedding() {
    // required by JPA
  }

  IdentityEmbedding(String identityId, String idNumber, List<Double> embedding) {
    this.identityId = identityId;
    this.idNumber = idNumber;
    this.embedding = embedding;
    this.updatedAt = Instant.now();
  }

  String getIdentityId() {
    return identityId;
  }

  List<Double> getEmbedding() {
    return embedding;
  }

  void updateEmbedding(List<Double> embedding) {
    this.embedding = embedding;
    this.updatedAt = Instant.now();
  }
}
