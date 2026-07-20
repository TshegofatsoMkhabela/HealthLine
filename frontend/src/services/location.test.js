import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  getBrowserLocation,
  generatePlusCode,
  isAccurateEnough,
  ACCURACY_TRUST_THRESHOLD_METERS,
} from "./location";

describe("generatePlusCode", () => {
  it("returns the exact Plus Code for a known reference coordinate", () => {
    // Reference vector from Google's own open-location-code test data
    // (test_data/encoding.csv), not invented — a mismatch here means we're
    // calling the library wrong, not that the library itself is broken.
    const code = generatePlusCode({ lat: 20.3700625, lng: 2.7821875 });

    expect(code).toBe("7FG49QCJ+2V");
  });
});

describe("isAccurateEnough", () => {
  it("accepts a fix exactly at the threshold", () => {
    expect(isAccurateEnough(ACCURACY_TRUST_THRESHOLD_METERS)).toBe(true);
  });

  it("rejects a fix worse than the threshold", () => {
    expect(isAccurateEnough(ACCURACY_TRUST_THRESHOLD_METERS + 1)).toBe(false);
  });
});

describe("getBrowserLocation", () => {
  let watchCallback;
  let errorCallback;
  let clearWatchSpy;

  beforeEach(() => {
    watchCallback = null;
    errorCallback = null;
    clearWatchSpy = vi.fn();
    global.navigator.geolocation = {
      watchPosition: vi.fn((onSuccess, onError) => {
        watchCallback = onSuccess;
        errorCallback = onError;
        return 1;
      }),
      clearWatch: clearWatchSpy,
    };
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function emit(accuracy) {
    watchCallback({
      coords: { latitude: -26.1, longitude: 28.05, accuracy },
    });
  }

  it("returns the reading with the lowest accuracy value after sampling multiple readings", async () => {
    const resultPromise = getBrowserLocation();

    emit(80);
    emit(15);
    emit(40);
    await vi.runAllTimersAsync();

    const result = await resultPromise;

    expect(result).toEqual({
      ok: true,
      coords: { lat: -26.1, lng: 28.05 },
      accuracy: 15,
    });
  });

  it("resolves ok:false immediately when geolocation is unavailable", async () => {
    global.navigator.geolocation = undefined;

    const result = await getBrowserLocation();

    expect(result).toEqual({ ok: false, reason: "Geolocation not supported." });
  });

  it("clears the watch once the sampling window ends", async () => {
    const resultPromise = getBrowserLocation();

    emit(20);
    await vi.runAllTimersAsync();
    await resultPromise;

    expect(clearWatchSpy).toHaveBeenCalledWith(1);
  });

  it("resolves immediately with a specific reason when permission is denied, without waiting out the sampling window", async () => {
    const resultPromise = getBrowserLocation();

    errorCallback({ code: 1, PERMISSION_DENIED: 1 });
    const result = await resultPromise;

    expect(result).toEqual({ ok: false, reason: "Location permission denied." });
    expect(clearWatchSpy).toHaveBeenCalledWith(1);
  });
});
