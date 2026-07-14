package com.healthline.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Shared wiring for the three health/actuator integration tests: same boot mode, same profile, same
 * "hit a path on this running instance" helper. Test-specific setup (extra profiles, property
 * overrides) stays on each subclass.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractHealthIntegrationTest {

  @LocalServerPort private int port;

  private final TestRestTemplate rest = new TestRestTemplate();

  protected ResponseEntity<String> getForEntity(String path) {
    return rest.getForEntity("http://localhost:" + port + path, String.class);
  }
}
