package com.healthline.backend.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface IdentityEmbeddingRepository extends JpaRepository<IdentityEmbedding, Long> {

  Optional<IdentityEmbedding> findByIdentityId(String identityId);

  Optional<IdentityEmbedding> findByIdNumber(String idNumber);
}
