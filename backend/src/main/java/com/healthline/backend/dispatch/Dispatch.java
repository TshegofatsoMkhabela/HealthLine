package com.healthline.backend.dispatch;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/**
 * In-memory record of one dispatch's lifecycle. Mutable and access is synchronized on the instance
 * because both the triggering request thread and the scheduler's delayed-transition thread can
 * touch the same dispatch (a cancel racing the window's expiry).
 *
 * <p>getStatus()/getPriorityCode()/getAiSummary() are synchronized per-getter, not under one
 * combined read — that's only coherent for a multi-field reader (e.g. DispatchResponseMapper)
 * because markDispatchedIfStillPending() is the sole writer of priorityCode/aiSummary, writes both
 * alongside status in the same synchronized block, and nothing ever un-sets them afterward. If a
 * future transition needs to overwrite priorityCode/aiSummary again after DISPATCHED (e.g. an
 * EN_ROUTE update), per-field locking stops being safe and reads need a single combined lock or an
 * immutable snapshot swapped via one field instead.
 */
class Dispatch {

  private final String dispatchId;
  private final Instant cancelWindowExpiresAt;
  private final TriagePayload triagePayload;
  private final LocationPayload location;
  private DispatchStatus status = DispatchStatus.PENDING;
  private ScheduledFuture<?> pendingTransition;
  private String priorityCode;
  private String aiSummary;

  Dispatch(
      String dispatchId,
      Instant cancelWindowExpiresAt,
      TriagePayload triagePayload,
      LocationPayload location) {
    this.dispatchId = dispatchId;
    this.cancelWindowExpiresAt = cancelWindowExpiresAt;
    this.triagePayload = triagePayload;
    this.location = location;
  }

  String getDispatchId() {
    return dispatchId;
  }

  Instant getCancelWindowExpiresAt() {
    return cancelWindowExpiresAt;
  }

  TriagePayload getTriagePayload() {
    return triagePayload;
  }

  LocationPayload getLocation() {
    return location;
  }

  synchronized DispatchStatus getStatus() {
    return status;
  }

  synchronized String getPriorityCode() {
    return priorityCode;
  }

  synchronized String getAiSummary() {
    return aiSummary;
  }

  synchronized void setPendingTransition(ScheduledFuture<?> pendingTransition) {
    this.pendingTransition = pendingTransition;
  }

  /**
   * Flips PENDING -> DISPATCHED and records the AI-generated content. No-op if the window already
   * resolved (cancel or a re-fire).
   */
  synchronized void markDispatchedIfStillPending(String priorityCode, String aiSummary) {
    if (status == DispatchStatus.PENDING) {
      status = DispatchStatus.DISPATCHED;
      this.priorityCode = priorityCode;
      this.aiSummary = aiSummary;
    }
  }

  /**
   * Flips PENDING -> CANCELLED and stops the scheduled auto-dispatch. No-op if the window already
   * closed — the caller always gets back whatever the current true state is.
   */
  synchronized void cancelIfStillPending() {
    if (status == DispatchStatus.PENDING) {
      status = DispatchStatus.CANCELLED;
      if (pendingTransition != null) {
        pendingTransition.cancel(false);
      }
    }
  }
}
