package com.healthline.backend.identity;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Stands in for Smile ID's Enhanced KYC sandbox until real credentials are available. See
 * specs/SPEC-login-biometrics-ekyc.md — the real SmileIdClient is an open item.
 */
@Component
public class MockSmileIdVerifier implements IdentityVerifier {

  private static final String VERIFIER_LABEL = "MOCK";
  private static final Pattern THIRTEEN_DIGITS = Pattern.compile("\\d{13}");

  @Override
  public VerificationResult verify(String idNumber, String selfieBlob) {
    if (idNumber == null || !THIRTEEN_DIGITS.matcher(idNumber).matches()) {
      return VerificationResult.failure(VERIFIER_LABEL, "ID number must be 13 digits.");
    }
    if (selfieBlob == null || selfieBlob.isBlank()) {
      return VerificationResult.failure(VERIFIER_LABEL, "Liveness selfie is required.");
    }
    return VerificationResult.success(VERIFIER_LABEL);
  }
}
