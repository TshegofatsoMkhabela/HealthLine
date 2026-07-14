package com.healthline.backend.config;

import java.util.Map;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owns /health at the site root, separate from Actuator's own default route at /actuator/health.
 * Actuator's exposure settings are left at their defaults (health only, under /actuator) rather
 * than touching base-path — setting base-path=/ would expose every future actuator endpoint at the
 * site root, not just health, and excluding web exposure entirely turned out to prevent the
 * HealthEndpoint bean from being created at all (its creation is conditional on being exposed via
 * some technology). Leaving Actuator's defaults alone and adding this controller alongside it
 * avoids both problems: two routes, same underlying check, no namespace risk either way.
 *
 * <p>healthEndpoint.health() returns full component details (DB validation query, disk paths, ...)
 * unconditionally — calling it directly bypasses show-details=when-authorized, which only applies
 * to Actuator's own auto-configured web endpoint. This is a public, unauthenticated route, so only
 * the aggregate status is returned; nothing internal leaks to an anonymous caller.
 *
 * <p>Status-to-HTTP-code mapping is delegated to Spring's own HttpCodeStatusMapper (the same bean
 * Actuator's own /actuator/health route uses) rather than a hardcoded UP-vs-everything-else check,
 * so both routes stay in sync if management.endpoint.health.status.http-mapping is ever customized.
 */
@RestController
public class HealthController {

  private final HealthEndpoint healthEndpoint;
  private final HttpCodeStatusMapper statusMapper;

  public HealthController(HealthEndpoint healthEndpoint, HttpCodeStatusMapper statusMapper) {
    this.healthEndpoint = healthEndpoint;
    this.statusMapper = statusMapper;
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Status status = healthEndpoint.health().getStatus();
    return ResponseEntity.status(statusMapper.getStatusCode(status))
        .body(Map.of("status", status.getCode()));
  }
}
