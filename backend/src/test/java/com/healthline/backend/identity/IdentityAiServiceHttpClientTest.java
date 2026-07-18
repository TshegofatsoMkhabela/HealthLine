package com.healthline.backend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class IdentityAiServiceHttpClientTest {

  private static final String BASE_URL = "http://ai-service.test";

  private MockRestServiceServer mockServer;
  private IdentityAiServiceHttpClient client;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    client = new IdentityAiServiceHttpClient(restTemplate, BASE_URL);
  }

  @Test
  void embedReturnsTheEmbeddingVectorParsedFromAiServicesResponse() {
    mockServer
        .expect(requestTo(BASE_URL + "/identity/embed"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"embedding\": [0.1, 0.2, 0.3]}", MediaType.APPLICATION_JSON));

    List<Double> embedding = client.embed("selfie-bytes");

    assertThat(embedding).containsExactly(0.1, 0.2, 0.3);
  }

  @Test
  void compareReturnsMatchTrueAndDistanceWhenAiServiceReportsAMatch() {
    mockServer
        .expect(requestTo(BASE_URL + "/identity/compare"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess("{\"match\": true, \"distance\": 0.12}", MediaType.APPLICATION_JSON));

    CompareResult result = client.compare("selfie-bytes", List.of(0.1, 0.2));

    assertThat(result.match()).isTrue();
    assertThat(result.distance()).isEqualTo(0.12);
  }

  @Test
  void compareReturnsMatchFalseAndDistanceWhenAiServiceReportsNoMatch() {
    mockServer
        .expect(requestTo(BASE_URL + "/identity/compare"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess("{\"match\": false, \"distance\": 0.85}", MediaType.APPLICATION_JSON));

    CompareResult result = client.compare("selfie-bytes", List.of(0.1, 0.2));

    assertThat(result.match()).isFalse();
    assertThat(result.distance()).isEqualTo(0.85);
  }

  @Test
  void compareSendsTheStoredEmbeddingAsANumericJsonArrayInTheRequestBody() {
    mockServer
        .expect(requestTo(BASE_URL + "/identity/compare"))
        .andExpect(
            content()
                .json("{\"selfieBlob\": \"selfie-bytes\", \"storedEmbedding\": [0.1, 0.2]}"))
        .andRespond(withSuccess("{\"match\": true, \"distance\": 0.0}", MediaType.APPLICATION_JSON));

    client.compare("selfie-bytes", List.of(0.1, 0.2));

    mockServer.verify();
  }
}
