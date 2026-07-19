package com.healthline.backend.identity;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record RecheckResponse(String identityId, Boolean match, Double distance, String error) {

  static RecheckResponse notFound(String identityId) {
    return new RecheckResponse(null, null, null, "No enrolled identity found for " + identityId);
  }

  static RecheckResponse badRequest(String reason) {
    return new RecheckResponse(null, null, null, reason);
  }

  static RecheckResponse serviceUnavailable() {
    return new RecheckResponse(
        null, null, null, "Identity service temporarily unavailable — try again shortly.");
  }

  static RecheckResponse result(String identityId, boolean match, double distance) {
    return new RecheckResponse(identityId, match, distance, null);
  }
}
