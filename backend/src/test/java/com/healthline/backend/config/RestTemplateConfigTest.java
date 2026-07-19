package com.healthline.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * A cold or hanging ai-service must not block a backend request thread forever (see
 * research/deepface-login-recheck.md's cold-start concern) — this proves the configured
 * RestTemplate actually gives up within a bounded time rather than relying on JVM/OS defaults,
 * which are often several minutes.
 */
class RestTemplateConfigTest {

  @Test
  void restTemplateGivesUpReadingWithinASmallBoundedWindowRatherThanHangingForever()
      throws IOException {
    RestTemplate restTemplate =
        new RestTemplateConfig()
            .restTemplate(new RestTemplateBuilder(), Duration.ofSeconds(5), Duration.ofSeconds(15));

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      Thread acceptButNeverRespond =
          new Thread(
              () -> {
                try (Socket ignored = serverSocket.accept()) {
                  Thread.sleep(60_000);
                } catch (Exception ignored) {
                  // server thread dies with the test; nothing to handle
                }
              });
      acceptButNeverRespond.setDaemon(true);
      acceptButNeverRespond.start();

      long start = System.currentTimeMillis();
      assertThatThrownBy(
              () -> restTemplate.getForEntity("http://localhost:" + port + "/", String.class))
          .isInstanceOf(ResourceAccessException.class);
      long elapsedMs = System.currentTimeMillis() - start;

      assertThat(elapsedMs).isLessThan(30_000);
    }
  }
}
