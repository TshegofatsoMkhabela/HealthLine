package com.healthline.backend.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Adversarial-review finding: location.plusCode was accepted by POST /trigger but silently
 * discarded — never stored anywhere, so nothing actually proved the spec's own claim that a payload
 * "can carry location data" through the chain. This proves it's threaded through the Dispatch
 * object, without touching the already-confirmed HTTP response contract (which deliberately doesn't
 * include location — see specs/SPEC-steel-thread.md).
 */
class DispatchServiceLocationTest {

  @Test
  void triggerStoresTheGivenLocationOnTheDispatch() {
    DispatchService service =
        new DispatchService(mock(AiServiceClient.class), mock(DispatchNotifier.class));
    TriagePayload triagePayload =
        new TriagePayload(21, "AB+", List.of(), List.of(), List.of(), List.of());
    LocationPayload location = new LocationPayload("8FW4V75V+8Q");

    Dispatch dispatch = service.trigger(triagePayload, location);

    assertThat(dispatch.getLocation()).isEqualTo(location);
    assertThat(dispatch.getLocation().plusCode()).isEqualTo("8FW4V75V+8Q");
  }
}
