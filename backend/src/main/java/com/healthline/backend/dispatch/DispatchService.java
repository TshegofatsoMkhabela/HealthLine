package com.healthline.backend.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Owns the dispatch state machine: PENDING -> DISPATCHED (auto, after the cancel window, carrying
 * an AI-generated triage summary) or PENDING -> CANCELLED (explicit). In-memory only for this
 * slice. See specs/SPEC-steel-thread.md.
 */
@Service
class DispatchService {

  private static final Duration CANCEL_WINDOW = Duration.ofSeconds(5);

  // ai-service being unreachable must never leave a dispatch stuck PENDING — a demo where the
  // panic button silently hangs is worse than one that shows a generic fallback message.
  private static final String FALLBACK_PRIORITY_CODE = "CODE_YELLOW";
  private static final String FALLBACK_SUMMARY = "Triage summary unavailable — see raw payload";

  // A small pool, not a single thread: completeDispatch() blocks on the ai-service HTTP call
  // (bounded by AiServiceHttpClient's own timeout, but still real wall-clock time), so a single
  // thread would serialize every concurrent dispatch's completion behind whichever one is
  // mid-call, even though each dispatch's own cancel window has already independently expired.
  private static final int SCHEDULER_POOL_SIZE = 4;

  private final Map<String, Dispatch> dispatches = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(SCHEDULER_POOL_SIZE);
  private final AiServiceClient aiServiceClient;
  private final DispatchNotifier notifier;

  DispatchService(AiServiceClient aiServiceClient, DispatchNotifier notifier) {
    this.aiServiceClient = aiServiceClient;
    this.notifier = notifier;
  }

  Dispatch trigger(TriagePayload triagePayload, LocationPayload location) {
    String dispatchId = UUID.randomUUID().toString();
    Dispatch dispatch =
        new Dispatch(dispatchId, Instant.now().plus(CANCEL_WINDOW), triagePayload, location);
    dispatches.put(dispatchId, dispatch);
    var future =
        scheduler.schedule(
            () -> completeDispatch(dispatchId), CANCEL_WINDOW.toMillis(), TimeUnit.MILLISECONDS);
    dispatch.setPendingTransition(future);
    notifier.notify(dispatch);
    return dispatch;
  }

  Optional<Dispatch> get(String dispatchId) {
    return Optional.ofNullable(dispatches.get(dispatchId));
  }

  Optional<Dispatch> cancel(String dispatchId) {
    Dispatch dispatch = dispatches.get(dispatchId);
    if (dispatch == null) {
      return Optional.empty();
    }
    dispatch.cancelIfStillPending();
    notifier.notify(dispatch);
    return Optional.of(dispatch);
  }

  private void completeDispatch(String dispatchId) {
    Dispatch dispatch = dispatches.get(dispatchId);
    if (dispatch == null) {
      return;
    }
    // Catches RestClientException specifically (timeout, connection refused, HTTP error from
    // ai-service) — not RuntimeException broadly, so a real bug in our own code (e.g. a
    // NullPointerException from malformed payload construction) surfaces as a failure instead of
    // silently reading as "ai-service is just unavailable."
    TriageSummaryResult result;
    try {
      result = aiServiceClient.summarize(dispatch.getTriagePayload());
    } catch (RestClientException e) {
      result = new TriageSummaryResult(FALLBACK_SUMMARY, FALLBACK_PRIORITY_CODE);
    }
    dispatch.markDispatchedIfStillPending(result.priorityCode(), result.summary());
    notifier.notify(dispatch);
  }
}
