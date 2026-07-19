package com.healthline.backend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public abstract class AbstractHealthIntegrationTest {

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

  protected <T> ResponseEntity<T> exchange(
      String path, HttpMethod method, Object body, Class<T> responseType) {
    HttpEntity<Object> entity = new HttpEntity<>(body);
    return rest.exchange("http://localhost:" + port + path, method, entity, responseType);
  }

  protected ResponseEntity<Map> postForEntity(String path, Object body) {
    return exchange(path, HttpMethod.POST, body, Map.class);
  }

  /**
   * A valid trigger payload with sensible empty-list defaults for every triagePayload field —
   * override only what a given test actually cares about, e.g. {@code
   * validTriagePayload(Map.of("allergies", List.of("Penicillin")))}.
   */
  protected Map<String, Object> validTriagePayload(Map<String, Object> triagePayloadOverrides) {
    Map<String, Object> triagePayload = new LinkedHashMap<>();
    triagePayload.put("age", 21);
    triagePayload.put("bloodType", "AB+");
    triagePayload.put("allergies", List.of());
    triagePayload.put("medications", List.of());
    triagePayload.put("chronicConditions", List.of());
    triagePayload.put("specialNeeds", List.of());
    triagePayload.putAll(triagePayloadOverrides);
    return Map.of(
        "triagePayload", triagePayload, "location", Collections.singletonMap("plusCode", null));
  }

  protected Map<String, Object> validTriagePayload() {
    return validTriagePayload(Map.of());
  }
}
