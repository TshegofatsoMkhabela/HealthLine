package com.healthline.backend.dispatch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
class AiServiceHttpClient implements AiServiceClient {

  // A slow (not down) ai-service must fail fast into DispatchService's fallback, not hang the
  // single scheduler thread that every dispatch's completion depends on — see the incident this
  // fixed: a Render cold-start-slow response with no timeout blocked that one thread forever,
  // silently breaking every dispatch, not just the one waiting on it.
  private static final int CONNECT_TIMEOUT_MS = 3000;
  private static final int READ_TIMEOUT_MS = 4000;

  private final RestTemplate restTemplate;
  private final String baseUrl;

  AiServiceHttpClient(@Value("${ai-service.base-url}") String baseUrl) {
    this.baseUrl = baseUrl;
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MS);
    this.restTemplate = new RestTemplate(requestFactory);
  }

  @Override
  public TriageSummaryResult summarize(TriagePayload payload) {
    return restTemplate.postForObject(
        baseUrl + "/triage/summarize", payload, TriageSummaryResult.class);
  }
}
