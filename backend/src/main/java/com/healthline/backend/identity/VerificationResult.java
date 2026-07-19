package com.healthline.backend.identity;

import java.time.Instant;

/**
 * Result of an identity verification attempt. {@code verifier} always identifies which
 * implementation produced this result ("MOCK" or "smile_id"), so no caller can mistake a mocked
 * result for a real one without it being visible in the data itself.
 */
public record VerificationResult(boolean ok, String verifier, Instant verifiedAt, String reason) {

  static VerificationResult success(String verifier) {
    return new VerificationResult(true, verifier, Instant.now(), null);
  }

  static VerificationResult failure(String verifier, String reason) {
    return new VerificationResult(false, verifier, null, reason);
  }
}
