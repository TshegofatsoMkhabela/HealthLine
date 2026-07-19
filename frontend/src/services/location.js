// Real what3words integration: call their REST API directly, e.g.
//   GET https://api.what3words.com/v3/convert-to-coordinates?words=...&key=...
// Kept mocked here so the frontend doesn't depend on a live API key yet.

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
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({ ok: false, reason: "Geolocation not supported." });
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) =>
        resolve({
          ok: true,
          coords: { lat: pos.coords.latitude, lng: pos.coords.longitude },
        }),
      () => resolve({ ok: false, reason: "Location permission denied." })
    );
  });
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

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
