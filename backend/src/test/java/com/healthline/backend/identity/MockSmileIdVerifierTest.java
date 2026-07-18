package com.healthline.backend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MockSmileIdVerifierTest {

  private final MockSmileIdVerifier verifier = new MockSmileIdVerifier();

  @Test
  void verifyReturnsOkTrueForValidIdNumberAndSelfie() {
    VerificationResult result = verifier.verify("0000000000000", "selfie-bytes");

    assertThat(result.ok()).isTrue();
    assertThat(result.verifier()).isEqualTo("MOCK");
    assertThat(result.verifiedAt()).isNotNull();
  }

  @Test
  void verifyReturnsOkFalseWithFormatReasonForIdNumberNotThirteenDigits() {
    VerificationResult tooShort = verifier.verify("123", "selfie-bytes");
    VerificationResult withLetters = verifier.verify("12345678901ab", "selfie-bytes");

    assertThat(tooShort.ok()).isFalse();
    assertThat(tooShort.reason()).containsIgnoringCase("13 digits");
    assertThat(withLetters.ok()).isFalse();
    assertThat(withLetters.reason()).containsIgnoringCase("13 digits");
  }

  @Test
  void verifyReturnsOkFalseWithSelfieReasonForMissingSelfie() {
    VerificationResult nullSelfie = verifier.verify("0000000000000", null);
    VerificationResult emptySelfie = verifier.verify("0000000000000", "");

    assertThat(nullSelfie.ok()).isFalse();
    assertThat(nullSelfie.reason()).containsIgnoringCase("selfie");
    assertThat(emptySelfie.ok()).isFalse();
    assertThat(emptySelfie.reason()).containsIgnoringCase("selfie");
  }

  @Test
  void verifyAlwaysLabelsTheResultAsMockRegardlessOfOutcome() {
    VerificationResult success = verifier.verify("0000000000000", "selfie-bytes");
    VerificationResult failure = verifier.verify("bad-id", "selfie-bytes");

    assertThat(success.verifier()).isEqualTo("MOCK");
    assertThat(failure.verifier()).isEqualTo("MOCK");
  }

  @Test
  void verifyIsStatelessAcrossRepeatedCallsWithTheSameInput() {
    VerificationResult first = verifier.verify("0000000000000", "selfie-bytes");
    VerificationResult second = verifier.verify("0000000000000", "selfie-bytes");

    assertThat(first.ok()).isTrue();
    assertThat(second.ok()).isTrue();
  }
}
