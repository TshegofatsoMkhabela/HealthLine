// Real what3words integration: call their REST API directly, e.g.
//   GET https://api.what3words.com/v3/convert-to-coordinates?words=...&key=...
// Kept mocked here so the frontend doesn't depend on a live API key yet.
import { wait } from "./mockNetwork";
import { OpenLocationCode } from "open-location-code";

const openLocationCode = new OpenLocationCode();

// GPS chips take a few seconds to "warm up" to their best accuracy — a single
// getCurrentPosition() call can return an early, worse-than-final fix. Sampling
// watchPosition() for this window and keeping the best reading fixes that.
const SAMPLING_WINDOW_MS = 6000;

export async function resolveWhat3Words(words) {
  await wait(700);
  if (!words || words.split(".").length !== 3) {
    return { ok: false, reason: "Enter three dot-separated words." };
  }
  return {
    ok: true,
    words,
    coords: { lat: -26.1076 + Math.random() * 0.01, lng: 28.0567 + Math.random() * 0.01 },
  };
}

export async function getBrowserLocation() {
  if (!navigator.geolocation) {
    return { ok: false, reason: "Geolocation not supported." };
  }

  return new Promise((resolve) => {
    let best = null;
    let windowTimer = null;

    const finish = (result) => {
      clearTimeout(windowTimer);
      navigator.geolocation.clearWatch(watchId);
      resolve(result);
    };

    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const { latitude, longitude, accuracy } = pos.coords;
        if (!best || accuracy < best.accuracy) {
          best = { lat: latitude, lng: longitude, accuracy };
        }
      },
      (err) => {
        // Every error code except TIMEOUT is definitive and non-transient
        // (permission denied, or position unavailable e.g. location services
        // off system-wide) — waiting out the rest of the sampling window
        // before telling the user would just be a silent multi-second stall
        // for no benefit. TIMEOUT alone is expected sampling noise (this
        // particular watchPosition callback didn't get a fix in time, but a
        // later one still can), so that's the only code still swallowed.
        if (err.code !== err.TIMEOUT) {
          const reason =
            err.code === err.PERMISSION_DENIED
              ? "Location permission denied."
              : "Could not get a location fix.";
          finish({ ok: false, reason });
        }
      },
      // This timeout bounds a single watchPosition callback, not the overall
      // sampling window below — they're independent knobs that happen to share
      // a value today. Don't delete one assuming it duplicates the other.
      { enableHighAccuracy: true, timeout: SAMPLING_WINDOW_MS, maximumAge: 0 },
    );

    windowTimer = setTimeout(() => {
      if (best) {
        finish({ ok: true, coords: { lat: best.lat, lng: best.lng }, accuracy: best.accuracy });
      } else {
        finish({ ok: false, reason: "Could not get a location fix." });
      }
    }, SAMPLING_WINDOW_MS);
  });
}

export function generatePlusCode({ lat, lng }) {
  return openLocationCode.encode(lat, lng);
}

// A fix worse than this isn't trustworthy enough to dispatch on. 50m is loose
// enough for typical GPS (7-13m normal, up to ~50m in moderate urban-canyon
// conditions) but tight enough to reject genuinely bad severe-urban-canyon
// fixes (200m+). See research/device-location-accuracy.md. Exported here (not
// left as a page-local constant) so every caller of getBrowserLocation applies
// the same trust rule rather than each redefining its own threshold.
export const ACCURACY_TRUST_THRESHOLD_METERS = 50;

export function isAccurateEnough(accuracy) {
  return accuracy <= ACCURACY_TRUST_THRESHOLD_METERS;
}

// Indoor Positioning System — no real Wi-Fi RTT hardware in a hackathon
// setting, so this simulates a floor/room reading. Replace with a real
// IPS SDK call when available.
export async function getIndoorPosition() {
  await wait(900);
  const floors = ["Ground", "1st", "2nd", "3rd"];
  const rooms = ["Room A", "Room B", "Corridor 2", "Room C"];
  return {
    ok: true,
    floor: floors[Math.floor(Math.random() * floors.length)],
    room: rooms[Math.floor(Math.random() * rooms.length)],
  };
}
