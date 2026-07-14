package com.healthline.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthEndpointTest extends AbstractHealthIntegrationTest {

  @Test
  void healthEndpointReturns200WhenDatabaseIsReachable() {
    ResponseEntity<String> response = getForEntity("/health");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }
}
