import { createContext, useContext, useState } from "react";

// Single source of truth for the whole demo flow. In a real build this
// would be split up and backed by real API calls (see src/services/*) —
// for now every service function returns mocked data so the frontend
// can be built and demoed independently of the backend.

const AppContext = createContext(null);

const initialState = {
  identity: {
    status: "unverified", // unverified | pending | verified
    idNumber: null,
  },
  profile: {
    status: "none", // none | draft | clinically-verified
    triage: {
      bloodType: "",
      allergies: "",
      medications: "",
      conditions: "",
    },
    fullRecord: {
      surgeries: "",
      familyHistory: "",
      gp: "",
      nextOfKin: "",
    },
  },
  location: {
    status: "idle", // idle | locating | locked
    what3words: null,
    coords: null,
    indoorMode: false,
    indoorPosition: null, // { floor, room }
  },
  triageResult: {
    priorityCode: null, // e.g. "Code Red — Respiratory Arrest"
    answers: {},
  },
  dispatch: {
    status: "idle", // idle | routing | en-route | arrived
    channel: null, // "private" | "public"
    provider: null,
    etaMinutes: null,
  },
  connection: "online", // online | offline — drives the status rail
};

export function AppProvider({ children }) {
  const [state, setState] = useState(initialState);

  const update = (path, value) => {
    setState((prev) => {
      const next = structuredClone(prev);
      const keys = path.split(".");
      let node = next;
      for (let i = 0; i < keys.length - 1; i++) node = node[keys[i]];
      node[keys[keys.length - 1]] = value;
      return next;
    });
  };

  return (
    <AppContext.Provider value={{ state, setState, update }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used inside AppProvider");
  return ctx;
}
