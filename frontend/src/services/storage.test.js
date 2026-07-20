import { describe, it, expect, beforeEach } from "vitest";
import { openDB } from "idb";
import { saveEncrypted, loadDecrypted, STORES } from "./storage";
import { deleteDb } from "../test/indexedDbTestUtils";

const DB_NAME = "medical-passport";

describe("storage", () => {
  beforeEach(async () => {
    await deleteDb(DB_NAME);
  });

  it("round-trips a saved value through loadDecrypted unchanged", async () => {
    const triage = { bloodType: "O+", allergies: "Penicillin" };

    await saveEncrypted(STORES.TRIAGE, triage);
    const loaded = await loadDecrypted(STORES.TRIAGE);

    expect(loaded).toEqual(triage);
  });

  it("returns null from loadDecrypted when nothing has been saved for that store", async () => {
    const loaded = await loadDecrypted(STORES.TRIAGE);

    expect(loaded).toBeNull();
  });

  it("stores ciphertext in IndexedDB, not the plaintext value", async () => {
    const triage = { bloodType: "O+", allergies: "Penicillin — severe" };

    await saveEncrypted(STORES.TRIAGE, triage);

    // Bypass storage.js's own decrypt path entirely — open the raw database and
    // read the record exactly as it sits on disk, the way inspecting IndexedDB
    // in DevTools would.
    const db = await openDB(DB_NAME, 1);
    const record = await db.get(STORES.TRIAGE, "current");
    db.close();

    expect(record).toBeDefined();
    // byteLength (not `instanceof ArrayBuffer`) — jsdom's test environment can
    // give crypto.subtle output an ArrayBuffer from a different realm than the
    // one this file's global `ArrayBuffer` points to, which makes `instanceof`
    // an unreliable check here even though the value genuinely is one.
    expect(record.ciphertext.byteLength).toBeGreaterThan(0);
    expect(record.iv.length).toBe(12);

    const rawBytes = new Uint8Array(record.ciphertext);
    const rawText = new TextDecoder().decode(rawBytes);
    expect(rawText).not.toContain("Penicillin");
    expect(rawText).not.toContain("O+");
  });

  it("overwrites rather than duplicates on a second save to the same store", async () => {
    await saveEncrypted(STORES.TRIAGE, { bloodType: "O+" });
    await saveEncrypted(STORES.TRIAGE, { bloodType: "AB-" });

    const loaded = await loadDecrypted(STORES.TRIAGE);
    expect(loaded).toEqual({ bloodType: "AB-" });

    const db = await openDB(DB_NAME, 1);
    const count = await db.count(STORES.TRIAGE);
    db.close();
    expect(count).toBe(1);
  });

  it("keeps the triage and fullRecord stores independent", async () => {
    await saveEncrypted(STORES.TRIAGE, { bloodType: "O+" });
    await saveEncrypted(STORES.FULL_RECORD, { surgeries: "Appendectomy" });

    expect(await loadDecrypted(STORES.TRIAGE)).toEqual({ bloodType: "O+" });
    expect(await loadDecrypted(STORES.FULL_RECORD)).toEqual({ surgeries: "Appendectomy" });
  });

  it("rejects an unknown store name instead of silently touching IndexedDB", async () => {
    await expect(saveEncrypted("triagee", { bloodType: "O+" })).rejects.toThrow(/Unknown store/);
    await expect(loadDecrypted("triagee")).rejects.toThrow(/Unknown store/);
  });
});
