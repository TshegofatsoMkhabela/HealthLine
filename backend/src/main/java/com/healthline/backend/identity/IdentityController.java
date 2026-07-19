package com.healthline.backend.identity;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/identity")
class IdentityController {

  private final IdentityVerifier verifier;
  private final IdentityAiServiceClient aiServiceClient;
  private final EmbeddingStore embeddingStore;

  IdentityController(
      IdentityVerifier verifier,
      IdentityAiServiceClient aiServiceClient,
      EmbeddingStore embeddingStore) {
    this.verifier = verifier;
    this.aiServiceClient = aiServiceClient;
    this.embeddingStore = embeddingStore;
  }

  @PostMapping("/enroll")
  ResponseEntity<EnrollResponse> enroll(@RequestBody EnrollRequest request) {
    VerificationResult result = verifier.verify(request.idNumber(), request.selfieBlob());
    if (!result.ok()) {
      return ResponseEntity.ok(EnrollResponse.rejected(result.reason()));
    }

    Optional<List<Double>> embedding =
        callAiService(() -> aiServiceClient.embed(request.selfieBlob()));
    if (embedding.isEmpty()) {
      String reason = "Identity service temporarily unavailable — try again shortly.";
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(EnrollResponse.error(reason));
    }

    // Re-enrollment by the same real person (same idNumber) reuses their existing
    // identityId and updates the embedding, rather than minting an unrelated duplicate.
    String identityId = embeddingStore.enrollOrReenroll(request.idNumber(), embedding.get());

    return ResponseEntity.ok(
        EnrollResponse.verified(identityId, result.verifiedAt().toString(), result.verifier()));
  }

  @PostMapping("/recheck")
  ResponseEntity<RecheckResponse> recheck(@RequestBody RecheckRequest request) {
    Optional<List<Double>> storedEmbedding = embeddingStore.find(request.identityId());
    if (storedEmbedding.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(RecheckResponse.notFound(request.identityId()));
    }

    if (request.selfieBlob() == null || request.selfieBlob().isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(RecheckResponse.badRequest("selfieBlob is required."));
    }

    Optional<CompareResult> result =
        callAiService(() -> aiServiceClient.compare(request.selfieBlob(), storedEmbedding.get()));
    if (result.isEmpty()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(RecheckResponse.serviceUnavailable());
    }

    return ResponseEntity.ok(
        RecheckResponse.result(
            request.identityId(), result.get().match(), result.get().distance()));
  }

  /**
   * ai-service being briefly unreachable (cold start, network blip) is expected, not a bug. Catches
   * RestClientException specifically, not RuntimeException broadly, so a real bug in
   * IdentityAiServiceHttpClient (e.g. a NullPointerException from a malformed response) surfaces as
   * a failure instead of silently reading as "ai-service is just unavailable."
   */
  private <T> Optional<T> callAiService(Supplier<T> call) {
    try {
      return Optional.of(call.get());
    } catch (RestClientException e) {
      return Optional.empty();
    }
  }
}
