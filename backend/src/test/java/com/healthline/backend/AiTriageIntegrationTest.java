package com.healthline.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.healthline.backend.dispatch.AiServiceClient;
import com.healthline.backend.dispatch.TriageSummaryResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

/**
 * Slice 2 of Issue 1: DISPATCHED responses carry real AI-generated content, with a safe fallback
 * when ai-service is unreachable so a dispatch never gets stuck PENDING. See
 * specs/SPEC-steel-thread.md.
 */
class AiTriageIntegrationTest extends AbstractHealthIntegrationTest {

  @MockBean private AiServiceClient aiServiceClient;

  @SuppressWarnings("unchecked")
  @Test
  void dispatchedResponseCarriesRealAiSummaryAndPriorityCode() throws InterruptedException {
    when(aiServiceClient.summarize(any()))
        .thenReturn(
            new TriageSummaryResult("21yo, AB+ — CRITICAL: Penicillin allergy", "CODE_RED"));

    Map<String, Object> triggerBody =
        postForEntity(
                "/api/emergency/trigger",
                validTriagePayload(Map.of("allergies", List.of("Penicillin"))))
            .getBody();
    String dispatchId = (String) triggerBody.get("dispatchId");

    Thread.sleep(6000);

    ResponseEntity<Map> response = getForEntityRaw("/api/emergency/" + dispatchId, Map.class);

    assertThat(response.getBody().get("status")).isEqualTo("DISPATCHED");
    assertThat(response.getBody().get("priorityCode")).isEqualTo("CODE_RED");
    assertThat(response.getBody().get("aiSummary"))
        .isEqualTo("21yo, AB+ — CRITICAL: Penicillin allergy");
  }

  @SuppressWarnings("unchecked")
  @Test
  void aiServiceFailureStillDispatchesWithFallbackContent() throws InterruptedException {
    when(aiServiceClient.summarize(any()))
        .thenThrow(new RestClientException("ai-service unreachable"));

    Map<String, Object> triggerBody =
        postForEntity("/api/emergency/trigger", validTriagePayload()).getBody();
    String dispatchId = (String) triggerBody.get("dispatchId");

    Thread.sleep(6000);

    ResponseEntity<Map> response = getForEntityRaw("/api/emergency/" + dispatchId, Map.class);

    assertThat(response.getBody().get("status")).isEqualTo("DISPATCHED");
    assertThat(response.getBody().get("priorityCode")).isEqualTo("CODE_YELLOW");
    assertThat(response.getBody().get("aiSummary")).isNotNull();
  }
}
