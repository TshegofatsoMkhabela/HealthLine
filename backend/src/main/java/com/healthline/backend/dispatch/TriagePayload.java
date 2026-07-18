package com.healthline.backend.dispatch;

import java.util.List;
import java.util.Set;

public record TriagePayload(
    Integer age,
    String bloodType,
    List<String> allergies,
    List<String> medications,
    List<String> chronicConditions,
    List<String> specialNeeds) {

  private static final Set<String> VALID_BLOOD_TYPES =
      Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

  public boolean hasValidBloodType() {
    return bloodType != null && VALID_BLOOD_TYPES.contains(bloodType);
  }
}
