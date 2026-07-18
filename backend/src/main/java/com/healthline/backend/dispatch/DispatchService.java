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

  private final Map<String, Dispatch> dispatches = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
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
    TriageSummaryResult result;
    try {
      result = aiServiceClient.summarize(dispatch.getTriagePayload());
    } catch (RuntimeException e) {
      result = new TriageSummaryResult(FALLBACK_SUMMARY, FALLBACK_PRIORITY_CODE);
    }
    dispatch.markDispatchedIfStillPending(result.priorityCode(), result.summary());
    notifier.notify(dispatch);
  }
}
