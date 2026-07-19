package com.healthline.backend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.healthline.backend.AbstractHealthIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Each test uses its own distinct idNumber. Requests here go through a real HTTP call
// (postForEntity, RANDOM_PORT) handled by a separate Tomcat thread, so @Transactional
// rollback would NOT undo their DB writes (it only wraps the test method's own thread) —
// distinct inputs per test is what actually keeps these independent, not a transaction.
class IdentityControllerTest extends AbstractHealthIntegrationTest {

  @MockBean private IdentityAiServiceClient aiServiceClient;

  @Autowired private EmbeddingStore embeddingStore;

  private static final List<Double> EMBEDDING = List.of(0.1, 0.2, 0.3);

  private String enrollAndGetIdentityId(String idNumber) {
    ResponseEntity<Map> response =
        postForEntity("/api/identity/enroll", Map.of("idNumber", idNumber, "selfieBlob", "selfie"));
    return (String) response.getBody().get("identityId");
  }

  @Test
  void enrollWithValidIdAndSelfieReturnsVerifiedAndStoresTheEmbedding() {
    when(aiServiceClient.embed(anyString())).thenReturn(EMBEDDING);

    ResponseEntity<Map> response =
        postForEntity(
            "/api/identity/enroll", Map.of("idNumber", "1000000000001", "selfieBlob", "selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> body = response.getBody();
    assertThat(body.get("status")).isEqualTo("verified");
    assertThat(body.get("verifier")).isEqualTo("MOCK");
    String identityId = (String) body.get("identityId");
    assertThat(identityId).isNotNull();
    assertThat(embeddingStore.find(identityId)).contains(EMBEDDING);
  }

  @Test
  void reEnrollingTheSameIdNumberReusesTheSameIdentityIdInsteadOfCreatingADuplicate() {
    when(aiServiceClient.embed(anyString())).thenReturn(EMBEDDING);
    String firstIdentityId = enrollAndGetIdentityId("1000000000002");

    List<Double> newEmbedding = List.of(0.4, 0.5, 0.6);
    when(aiServiceClient.embed(anyString())).thenReturn(newEmbedding);
    String secondIdentityId = enrollAndGetIdentityId("1000000000002");

    assertThat(secondIdentityId).isEqualTo(firstIdentityId);
    assertThat(embeddingStore.find(firstIdentityId)).contains(newEmbedding);
  }

  @Test
  void enrollReturns503WithoutStoringAnythingWhenTheAiServiceFails() {
    when(aiServiceClient.embed(anyString())).thenThrow(new RuntimeException("connection refused"));

    ResponseEntity<Map> response =
        postForEntity(
            "/api/identity/enroll", Map.of("idNumber", "1000000000003", "selfieBlob", "selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().get("status")).isEqualTo("error");
    assertThat(embeddingStore.findIdentityIdByIdNumber("1000000000003")).isEmpty();
  }

  @Test
  void enrollWithInvalidIdIsRejectedAndNeverCallsTheAiService() {
    ResponseEntity<Map> response =
        postForEntity("/api/identity/enroll", Map.of("idNumber", "123", "selfieBlob", "selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("status")).isEqualTo("rejected");
    assertThat(response.getBody().get("reason")).asString().containsIgnoringCase("13 digits");
    verifyNoInteractions(aiServiceClient);
  }

  @Test
  void recheckWithSamePersonReturnsMatchTrue() {
    when(aiServiceClient.embed(anyString())).thenReturn(EMBEDDING);
    String identityId = enrollAndGetIdentityId("1000000000004");
    when(aiServiceClient.compare(anyString(), any())).thenReturn(new CompareResult(true, 0.12));

    ResponseEntity<Map> response =
        postForEntity(
            "/api/identity/recheck", Map.of("identityId", identityId, "selfieBlob", "new-selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("match")).isEqualTo(true);
    assertThat((Double) response.getBody().get("distance")).isEqualTo(0.12);
  }

  @Test
  void recheckWithDifferentPersonReturnsMatchFalseNotAnError() {
    when(aiServiceClient.embed(anyString())).thenReturn(EMBEDDING);
    String identityId = enrollAndGetIdentityId("1000000000005");
    when(aiServiceClient.compare(anyString(), any())).thenReturn(new CompareResult(false, 0.9));

    ResponseEntity<Map> response =
        postForEntity(
            "/api/identity/recheck",
            Map.of("identityId", identityId, "selfieBlob", "stranger-selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("match")).isEqualTo(false);
  }

  @Test
  void recheckForAnUnknownIdentityReturns404AndNeverCallsCompare() {
    ResponseEntity<Map> response =
        postForEntity(
            "/api/identity/recheck",
            Map.of("identityId", "never-enrolled", "selfieBlob", "selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    verify(aiServiceClient, never()).compare(anyString(), any());
  }

  @Test
  void recheckWithMissingSelfieBlobReturns400AndNeverCallsCompare() {
    when(aiServiceClient.embed(anyString())).thenReturn(EMBEDDING);
    String identityId = enrollAndGetIdentityId("1000000000006");

    ResponseEntity<Map> response =
        postForEntity("/api/identity/recheck", Map.of("identityId", identityId));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(aiServiceClient, never()).compare(anyString(), any());
  }

  @Test
  void recheckReturns503WhenTheAiServiceFails() {
    when(aiServiceClient.embed(anyString())).thenReturn(EMBEDDING);
    String identityId = enrollAndGetIdentityId("1000000000007");
    when(aiServiceClient.compare(anyString(), any()))
        .thenThrow(new RuntimeException("connection refused"));

    ResponseEntity<Map> response =
        postForEntity(
            "/api/identity/recheck", Map.of("identityId", identityId, "selfieBlob", "selfie"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().get("error")).isNotNull();
  }
}
