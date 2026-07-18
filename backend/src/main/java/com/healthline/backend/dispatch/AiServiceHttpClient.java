package com.healthline.backend.dispatch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
class AiServiceHttpClient implements AiServiceClient {

  private final RestTemplate restTemplate = new RestTemplate();
  private final String baseUrl;

  AiServiceHttpClient(@Value("${ai-service.base-url}") String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public TriageSummaryResult summarize(TriagePayload payload) {
    return restTemplate.postForObject(
        baseUrl + "/triage/summarize", payload, TriageSummaryResult.class);
  }
}
