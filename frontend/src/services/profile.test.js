import { describe, it, expect, beforeEach } from "vitest";
import { saveProfile, loadProfile } from "./profile";
import { deleteDb } from "../test/indexedDbTestUtils";

describe("profile save/load (the actual API ProfileBuilder calls)", () => {
  beforeEach(async () => {
    await deleteDb("medical-passport");
  });

  it("loadProfile returns the same triage and fullRecord saveProfile was given", async () => {
    const triage = { bloodType: "O+", allergies: "Penicillin" };
    const fullRecord = { surgeries: "Appendectomy, 2019" };

    await saveProfile({ triage, fullRecord });
    const loaded = await loadProfile();

    expect(loaded).toEqual({ triage, fullRecord });
  });

  it("loadProfile returns null fields when nothing has ever been saved", async () => {
    const loaded = await loadProfile();

    expect(loaded).toEqual({ triage: null, fullRecord: null });
  });

  it("a second saveProfile call overwrites rather than duplicates", async () => {
    await saveProfile({ triage: { bloodType: "O+" }, fullRecord: { surgeries: "" } });
    await saveProfile({ triage: { bloodType: "AB-" }, fullRecord: { surgeries: "Appendectomy" } });

    const loaded = await loadProfile();
    expect(loaded).toEqual({
      triage: { bloodType: "AB-" },
      fullRecord: { surgeries: "Appendectomy" },
    });
  });
});
