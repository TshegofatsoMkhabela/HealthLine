// Simulates the routing decision: AURA (private aggregator) if a vetted
// ambulance is available nearby, otherwise a structured report to the
// 10177 public dispatcher dashboard. Also simulates the ETA ticking down
// and the hospital load-balancer decision.
import { wait } from "./mockNetwork";

export async function routeEmergency({ priorityCode, coords }) {
  await wait(1000);

  const privateAvailable = Math.random() > 0.35;

  if (privateAvailable) {
    return {
      ok: true,
      channel: "private",
      provider: "AURA",
      etaMinutes: 7 + Math.floor(Math.random() * 6),
    };
  }

  return {
    ok: true,
    channel: "public",
    provider: "10177 Dispatch",
    etaMinutes: 14 + Math.floor(Math.random() * 10),
    predicted: true,
  };
}

export async function findAcceptingHospital() {
  await wait(700);
  const hospitals = [
    { name: "Life Fourways Hospital", status: "accepting", distanceKm: 4.2 },
    { name: "Netcare Milpark", status: "diversion", distanceKm: 6.8 },
    { name: "Steve Biko Academic Hospital", status: "accepting", distanceKm: 9.1 },
  ];
  return { ok: true, hospitals, selected: hospitals.find((h) => h.status === "accepting") };
}
