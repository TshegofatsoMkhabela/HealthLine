package com.healthline.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Only health-related routes should be reachable. /actuator/health legitimately returns 200 — it's
 * Actuator's own default route, gated by show-details=when-authorized so it only ever reveals the
 * aggregate status to an anonymous caller, same as the plain /health route. What must never be
 * reachable is anything that leaks internals: config values (/env), the bean graph (/beans),
 * routing internals (/mappings), or component-level health details.
 */
class ActuatorExposureTest extends AbstractHealthIntegrationTest {

  @Test
  void envEndpointIsNotExposed() {
    assertNotFound("/env");
  }

  @Test
  void beansEndpointIsNotExposed() {
    assertNotFound("/beans");
  }

  @Test
  void mappingsEndpointIsNotExposed() {
    assertNotFound("/mappings");
  }

  @Test
  void actuatorHealthRevealsOnlyAggregateStatusToAnAnonymousCaller() {
    ResponseEntity<String> response = getForEntity("/actuator/health");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).doesNotContain("\"components\"", "\"details\"");
  }

  @Test
  void plainHealthRouteRevealsOnlyAggregateStatus() {
    ResponseEntity<String> response = getForEntity("/health");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).doesNotContain("\"components\"", "\"details\"");
  }

  private void assertNotFound(String path) {
    ResponseEntity<String> response = getForEntity(path);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
