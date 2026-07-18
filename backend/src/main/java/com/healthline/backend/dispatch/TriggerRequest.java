package com.healthline.backend.dispatch;

public record TriggerRequest(TriagePayload triagePayload, LocationPayload location) {}
