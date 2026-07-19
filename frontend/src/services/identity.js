// Stands in for the Smile ID -> DHA registry check described in the README.
// Swap the body of verifyIdentity() for a real fetch() once the backend
// endpoint exists — nothing outside this file needs to change.
import { wait } from "./mockNetwork";

export async function verifyIdentity({ idNumber, selfieBlob }) {
  await wait(1400);

  if (!idNumber || idNumber.length !== 13) {
    return { ok: false, reason: "ID number must be 13 digits." };
  }

  return {
    ok: true,
    verifiedAt: new Date().toISOString(),
  };
}
