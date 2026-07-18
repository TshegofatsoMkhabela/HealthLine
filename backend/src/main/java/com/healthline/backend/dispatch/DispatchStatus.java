package com.healthline.backend.dispatch;

public enum DispatchStatus {
  PENDING,
  DISPATCHED,

  // Declared per specs/SPEC-steel-thread.md's WebSocket section (a future mockResponderEtaMinutes
  // transition), but nothing currently produces it — DispatchService has no DISPATCHED -> EN_ROUTE
  // transition yet. Reserved for a later slice, not dead code; DispatchResponseMapper groups it
  // with DISPATCHED's field set in the meantime.
  EN_ROUTE,

  CANCELLED
}
