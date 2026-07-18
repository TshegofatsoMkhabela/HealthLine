package com.healthline.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Shared wiring for integration tests: same boot mode, same profile, same "hit a path on this
 * running instance" helpers. Test-specific setup (extra profiles, property overrides) stays on each
 * subclass.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractHealthIntegrationTest {

  @LocalServerPort private int port;

  private final TestRestTemplate rest = new TestRestTemplate();

  protected int getPort() {
    return port;
  }

  protected ResponseEntity<String> getForEntity(String path) {
    return rest.getForEntity("http://localhost:" + port + path, String.class);
  }

  protected <T> ResponseEntity<T> getForEntityRaw(String path, Class<T> responseType) {
    return rest.getForEntity("http://localhost:" + port + path, responseType);
  }

  protected ResponseEntity<java.util.Map> postForEntity(String path, Object body) {
    return rest.postForEntity("http://localhost:" + port + path, body, java.util.Map.class);
  }

  protected <T> ResponseEntity<T> exchange(
      String path, HttpMethod method, Object body, Class<T> responseType) {
    HttpEntity<Object> entity = new HttpEntity<>(body);
    return rest.exchange("http://localhost:" + port + path, method, entity, responseType);
  }
}
