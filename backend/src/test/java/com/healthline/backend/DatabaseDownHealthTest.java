package com.healthline.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Points the datasource at a port nothing is listening on (see application-db-down.properties for
 * why every boot-time DB touch is also disabled), so the app boots successfully but the actual
 * connection attempt only happens when /health is hit — reproducing "DB goes down after the app is
 * already running" rather than "DB was never reachable".
 */
@ActiveProfiles({"test", "db-down"})
class DatabaseDownHealthTest extends AbstractHealthIntegrationTest {

  @Test
  void healthEndpointReturns503WhenDatabaseIsUnreachable() {
    ResponseEntity<String> response = getForEntity("/health");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).contains("\"status\":\"DOWN\"");
  }
}
