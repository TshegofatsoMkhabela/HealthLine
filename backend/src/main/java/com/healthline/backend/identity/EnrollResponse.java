package com.healthline.backend.identity;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record EnrollResponse(
    String identityId, String status, String verifiedAt, String verifier, String reason) {

  static EnrollResponse rejected(String reason) {
    return new EnrollResponse(null, "rejected", null, null, reason);
  }

  static EnrollResponse error(String reason) {
    return new EnrollResponse(null, "error", null, null, reason);
  }

  static EnrollResponse verified(String identityId, String verifiedAt, String verifier) {
    return new EnrollResponse(identityId, "verified", verifiedAt, verifier, null);
  }
}
