package com.healthline.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Slice 1 of Issue 1 (steel thread): the trigger/poll/cancel API and its in-memory state machine.
 * No AURA, AI, or WebSocket calls yet — those are later slices. See specs/SPEC-steel-thread.md.
 */
class DispatchControllerTest extends AbstractHealthIntegrationTest {

  private Map<String, Object> validTriagePayload() {
    return Map.of(
        "triagePayload",
            Map.of(
                "age", 21,
                "bloodType", "AB+",
                "allergies", List.of("Penicillin", "Peanuts"),
                "medications", List.of("Metformin 500mg"),
                "chronicConditions", List.of("Type 2 Diabetes"),
                "specialNeeds", List.of("Wheelchair")),
        "location", Collections.singletonMap("plusCode", null));
  }

  @SuppressWarnings("unchecked")
  @Test
  void triggerReturns201WithPendingStatusAndCancelWindow() {
    ResponseEntity<Map> response = postForEntity("/api/emergency/trigger", validTriagePayload());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().toString()).startsWith("/api/emergency/");

    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("dispatchId")).isNotNull();
    assertThat(body.get("status")).isEqualTo("PENDING");
    assertThat(body.get("cancelWindowExpiresAt")).isNotNull();

    Instant expiresAt = Instant.parse((String) body.get("cancelWindowExpiresAt"));
    assertThat(expiresAt).isAfter(Instant.now()).isBefore(Instant.now().plusSeconds(6));
  }

  @Test
  void triggerWithInvalidBloodTypeReturns400AndCreatesNothing() {
    Map<String, Object> invalidPayload =
        Map.of(
            "triagePayload",
                Map.of(
                    "age", 21,
                    "bloodType", "X+",
                    "allergies", List.of(),
                    "medications", List.of(),
                    "chronicConditions", List.of(),
                    "specialNeeds", List.of()),
            "location", Collections.singletonMap("plusCode", null));

    ResponseEntity<Map> response = postForEntity("/api/emergency/trigger", invalidPayload);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getUnknownDispatchIdReturns404() {
    ResponseEntity<Map> response = getForEntityRaw("/api/emergency/does-not-exist", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @SuppressWarnings("unchecked")
  @Test
  void cancelWithinWindowReturnsCancelledAndGetAgrees() {
    Map<String, Object> triggerBody =
        postForEntity("/api/emergency/trigger", validTriagePayload()).getBody();
    String dispatchId = (String) triggerBody.get("dispatchId");

    ResponseEntity<Map> cancelResponse =
        exchange("/api/emergency/" + dispatchId + "/cancel", HttpMethod.POST, null, Map.class);

    assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelResponse.getBody().get("status")).isEqualTo("CANCELLED");

    ResponseEntity<Map> getResponse = getForEntityRaw("/api/emergency/" + dispatchId, Map.class);
    assertThat(getResponse.getBody().get("status")).isEqualTo("CANCELLED");
  }

  @SuppressWarnings("unchecked")
  @Test
  void cancelAfterWindowExpiredReturnsCurrentStateNotCancelled() throws InterruptedException {
    Map<String, Object> triggerBody =
        postForEntity("/api/emergency/trigger", validTriagePayload()).getBody();
    String dispatchId = (String) triggerBody.get("dispatchId");

    // Window is 5s; wait past it so the state machine has advanced to DISPATCHED.
    Thread.sleep(6000);

    ResponseEntity<Map> cancelResponse =
        exchange("/api/emergency/" + dispatchId + "/cancel", HttpMethod.POST, null, Map.class);

    assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelResponse.getBody().get("status")).isNotEqualTo("CANCELLED");
    assertThat(cancelResponse.getBody().get("status")).isEqualTo("DISPATCHED");
  }
}
