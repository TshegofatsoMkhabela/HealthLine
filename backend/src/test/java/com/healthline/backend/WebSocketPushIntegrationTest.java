package com.healthline.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Slice 3 of Issue 1: the WebSocket/STOMP push is a live-update enhancement layered on the
 * always-correct poll from Slice 1 — same response shape, same transitions, pushed instead of
 * pulled. See specs/SPEC-steel-thread.md.
 *
 * <p>Subscribes to a dispatch's topic *after* triggering it (missing that first PENDING push
 * deliberately) and asserts on the CANCELLED push instead — this avoids a race between "has the
 * subscription registered yet" and "did trigger's own push already fire," which a test asserting on
 * the trigger push itself would be exposed to.
 */
class WebSocketPushIntegrationTest extends AbstractHealthIntegrationTest {

  @SuppressWarnings("unchecked")
  @Test
  void cancelPushesCancelledMessageToSubscribedClient() throws Exception {
    WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());

    StompSession session =
        stompClient
            .connectAsync(
                "ws://localhost:" + getPort() + "/ws", new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    Map<String, Object> triggerBody =
        postForEntity("/api/emergency/trigger", validTriagePayload()).getBody();
    String dispatchId = (String) triggerBody.get("dispatchId");

    BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
    session.subscribe(
        "/topic/dispatch/" + dispatchId,
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            received.add((Map<String, Object>) payload);
          }
        });

    // The SUBSCRIBE frame above is sent asynchronously; give the broker a moment to register it
    // before triggering the message we're asserting on, to avoid a subscribe/publish race.
    Thread.sleep(500);

    exchange(
        "/api/emergency/" + dispatchId + "/cancel",
        org.springframework.http.HttpMethod.POST,
        null,
        Map.class);

    Map<String, Object> message = received.poll(5, TimeUnit.SECONDS);

    assertThat(message).isNotNull();
    assertThat(message.get("dispatchId")).isEqualTo(dispatchId);
    assertThat(message.get("status")).isEqualTo("CANCELLED");
  }
}
