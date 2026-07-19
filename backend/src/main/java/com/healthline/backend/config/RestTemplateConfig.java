package com.healthline.backend.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class RestTemplateConfig {

  // Bounded so a cold or hanging ai-service (a known, documented risk — see
  // research/deepface-login-recheck.md) fails a request within seconds instead of
  // blocking the calling thread for the JVM/OS default (often several minutes).
  // Externalized like ai-service.base-url so ops can tune per environment without a redeploy.
  @Bean
  RestTemplate restTemplate(
      RestTemplateBuilder builder,
      @Value("${ai-service.connect-timeout}") Duration connectTimeout,
      @Value("${ai-service.read-timeout}") Duration readTimeout) {
    return builder.setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).build();
  }
}
