// Handles saving the two-payload medical profile and the doctor sign-off
// step. Triage and Full EHR payloads are persisted separately and encrypted
// at rest via storage.js (Issue 3, POPIA data minimization) — this file only
// owns the mocked network delay and the sign-off flow, not storage details.
import { wait } from "./mockNetwork";
import { getOrCreateKey, saveEncrypted, loadDecrypted, STORES } from "./storage";

export async function saveProfile({ triage, fullRecord }) {
  await wait(600);
  // Resolved once and passed to both writes — they touch independent IndexedDB
  // stores, so they run in parallel, and neither needs its own key lookup.
  const key = await getOrCreateKey();
  await Promise.all([
    saveEncrypted(STORES.TRIAGE, triage, key),
    saveEncrypted(STORES.FULL_RECORD, fullRecord, key),
  ]);
  return { ok: true, savedAt: new Date().toISOString() };
}

export async function loadProfile() {
  const key = await getOrCreateKey();
  const [triage, fullRecord] = await Promise.all([
    loadDecrypted(STORES.TRIAGE, key),
    loadDecrypted(STORES.FULL_RECORD, key),
  ]);
  return { triage, fullRecord };
}

export async function requestDoctorSignOff({ hpcsaNumber }) {
  await wait(1200);

  if (!hpcsaNumber || hpcsaNumber.length < 4) {
    return { ok: false, reason: "Enter a valid HPCSA registration number." };
  }

  return {
    ok: true,
    signedBy: `Dr. (HPCSA ${hpcsaNumber})`,
    signedAt: new Date().toISOString(),
  };
}
