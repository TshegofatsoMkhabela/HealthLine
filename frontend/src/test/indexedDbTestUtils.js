// Shared by any test that needs a clean IndexedDB between test cases —
// fake-indexeddb (see src/test/setup.js) persists across tests within one
// file otherwise, since it's an in-memory polyfill keyed by database name,
// not reset automatically the way jsdom's DOM tree is.
export function deleteDb(dbName) {
  return new Promise((resolve, reject) => {
    const req = indexedDB.deleteDatabase(dbName);
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
    req.onblocked = () => resolve();
  });
}
