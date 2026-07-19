import { useApp } from "../state/AppContext";

const LIGHT_STYLES = {
  off: "bg-line",
  pending: "bg-signal-amber animate-pulse",
  on: "bg-verified-teal",
  alert: "bg-signal-red animate-pulse",
};

function Light({ label, state }) {
  return (
    <div className="flex items-center gap-2">
      <span className={`h-2.5 w-2.5 rounded-full ${LIGHT_STYLES[state]}`} />
      <span className="text-xs font-mono uppercase tracking-wide text-mist">
        {label}
      </span>
    </div>
  );
}

export default function StatusRail() {
  const { state } = useApp();

  const identityState =
    state.identity.status === "verified"
      ? "on"
      : state.identity.status === "pending"
      ? "pending"
      : "off";

  const locationState =
    state.location.status === "locked"
      ? "on"
      : state.location.status === "locating"
      ? "pending"
      : "off";

  const connectionState = state.connection === "online" ? "on" : "alert";

  return (
    <div className="w-full border-b border-line bg-panel px-4 py-2.5 flex items-center justify-between">
      <span className="font-display font-semibold text-sm tracking-tight text-paper">
        Medical Passport
      </span>
      <div className="flex items-center gap-4">
        <Light label="Identity" state={identityState} />
        <Light label="Location" state={locationState} />
        <Light label="Connection" state={connectionState} />
      </div>
    </div>
  );
}
