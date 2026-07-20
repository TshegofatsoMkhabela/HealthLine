// Real on-device encrypted storage for the split Triage / Full EHR payloads
// (Issue 3, POPIA data minimization). Two physically separate IndexedDB
// object stores plus a third holding the single AES-256-GCM key — generated
// once, non-extractable, so it can't be read back out as raw bytes even by
// code running in this same origin.
import { openDB } from "idb";

const DB_NAME = "medical-passport";
const DB_VERSION = 1;
const RECORD_ID = "current";
const KEY_RECORD_ID = "current";

// A closed, known set — not a free-form string — matching how AppContext.update()
// guards its own string-typed selectors against typos, per an earlier /simplify
// pass on this codebase. A typo'd store name throws immediately here with a
// clear message, instead of surfacing later as IndexedDB's generic NotFoundError
// far from the actual mistake.
export const STORES = { TRIAGE: "triage", FULL_RECORD: "fullRecord" };

function assertValidStore(storeName) {
  if (!Object.values(STORES).includes(storeName)) {
    throw new Error(
      `Unknown store "${storeName}" — expected one of: ${Object.values(STORES).join(", ")}`,
    );
  }
}

function db() {
  return openDB(DB_NAME, DB_VERSION, {
    upgrade(database) {
      database.createObjectStore(STORES.TRIAGE);
      database.createObjectStore(STORES.FULL_RECORD);
      database.createObjectStore("keys");
    },
  });
}

// Memoized per module load (i.e. per page session — a fresh page load starts
// fresh, IndexedDB is still the real cross-reload persistence). Without this,
// two concurrent first-ever callers of getOrCreateKey() could each see no key
// stored yet, each generate their own, and whichever's `put` lands last would
// silently win — leaving anything encrypted with the losing key permanently
// undecryptable. Memoizing the in-flight promise (not just the resolved key)
// means a second concurrent caller awaits the same generation instead of
// racing a second one.
let keyPromise = null;

export function getOrCreateKey() {
  if (!keyPromise) {
    keyPromise = (async () => {
      const database = await db();
      let key = await database.get("keys", KEY_RECORD_ID);
      if (!key) {
        key = await crypto.subtle.generateKey({ name: "AES-GCM", length: 256 }, false, [
          "encrypt",
          "decrypt",
        ]);
        await database.put("keys", key, KEY_RECORD_ID);
      }
      database.close();
      return key;
    })();
  }
  return keyPromise;
}

// `key` is optional — pass an already-resolved key (e.g. from a caller that's
// writing to multiple stores in one logical operation) to avoid looking it up
// again; omit it to have this function resolve it itself.
export async function saveEncrypted(storeName, value, key = null) {
  assertValidStore(storeName);
  const resolvedKey = key ?? (await getOrCreateKey());

  // A fresh random IV per encryption is required for AES-GCM — reusing an IV
  // with the same key breaks the cipher's security guarantees. The IV itself
  // isn't secret (only the key is), so it's stored alongside the ciphertext.
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const plaintext = new TextEncoder().encode(JSON.stringify(value));
  const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, resolvedKey, plaintext);

  const database = await db();
  await database.put(storeName, { iv, ciphertext }, RECORD_ID);
  database.close();
}

export async function loadDecrypted(storeName, key = null) {
  assertValidStore(storeName);

  const database = await db();
  const record = await database.get(storeName, RECORD_ID);
  database.close();
  if (!record) return null;

  const resolvedKey = key ?? (await getOrCreateKey());
  const plaintext = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: record.iv },
    resolvedKey,
    record.ciphertext,
  );
  return JSON.parse(new TextDecoder().decode(plaintext));
}
