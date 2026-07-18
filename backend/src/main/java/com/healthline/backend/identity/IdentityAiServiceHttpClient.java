package com.healthline.backend.identity;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
class IdentityAiServiceHttpClient implements IdentityAiServiceClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  IdentityAiServiceHttpClient(
      RestTemplate restTemplate, @Value("${ai-service.base-url}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
  }

  @Override
  public List<Double> embed(String selfieBlob) {
    EmbedResponse response =
        restTemplate.postForObject(
            baseUrl + "/identity/embed", new EmbedRequest(selfieBlob), EmbedResponse.class);
    return response.embedding();
  }

  @Override
  public CompareResult compare(String selfieBlob, List<Double> storedEmbedding) {
    CompareResponse response =
        restTemplate.postForObject(
            baseUrl + "/identity/compare",
            new CompareRequest(selfieBlob, storedEmbedding),
            CompareResponse.class);
    return new CompareResult(response.match(), response.distance());
  }

  private record EmbedRequest(String selfieBlob) {}

  private record EmbedResponse(List<Double> embedding) {}

  private record CompareRequest(String selfieBlob, List<Double> storedEmbedding) {}

  private record CompareResponse(boolean match, double distance) {}
}
