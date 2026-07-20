import "@testing-library/jest-dom/vitest";
// jsdom has no native IndexedDB — this polyfills global indexedDB/IDBKeyRange
// with an in-memory implementation so storage.js's real IndexedDB code can be
// tested without a browser.
import "fake-indexeddb/auto";
