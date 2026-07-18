package com.healthline.backend.identity;

/**
 * Verifies that an SA ID number + liveness selfie belong to a real, DHA-registered person.
 * Implementations are pure functions of their inputs — no stored state, no side effects.
 */
public interface IdentityVerifier {

  VerificationResult verify(String idNumber, String selfieBlob);
}
