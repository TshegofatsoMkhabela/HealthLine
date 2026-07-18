package com.healthline.backend.dispatch;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the one response shape shared by GET, POST /cancel, and the WebSocket push — per
 * specs/SPEC-steel-thread.md, all three describe "the current state" the same way.
 */
final class DispatchResponseMapper {

  private DispatchResponseMapper() {}

  static Map<String, Object> toBody(Dispatch dispatch) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("dispatchId", dispatch.getDispatchId());
    body.put("status", dispatch.getStatus().name());
    switch (dispatch.getStatus()) {
      case PENDING ->
          body.put("cancelWindowExpiresAt", dispatch.getCancelWindowExpiresAt().toString());
      case DISPATCHED, EN_ROUTE -> {
        body.put("priorityCode", dispatch.getPriorityCode());
        body.put("aiSummary", dispatch.getAiSummary());
      }
      case CANCELLED -> {
        // dispatchId + status only
      }
    }
    return body;
  }
}
