// Handles saving the two-payload medical profile and the doctor sign-off
// step. In production the triage + full record payloads would be encrypted
// on-device (POPIA requirement) — this mock just simulates the round trip.
import { wait } from "./mockNetwork";

export async function saveProfile({ triage, fullRecord }) {
  await wait(600);
  return { ok: true, savedAt: new Date().toISOString() };
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
