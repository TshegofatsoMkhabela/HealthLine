package com.healthline.backend.dispatch;

public interface AiServiceClient {
  TriageSummaryResult summarize(TriagePayload payload);
}
