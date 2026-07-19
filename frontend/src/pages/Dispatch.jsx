import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../state/AppContext";
import { routeEmergency } from "../services/dispatch";
import Card from "../components/Card";
import PriorityBadge from "../components/PriorityBadge";

export default function Dispatch() {
  const { state, update } = useApp();
  const navigate = useNavigate();
  const [routing, setRouting] = useState(true);

  useEffect(() => {
    let interval;
    (async () => {
      update("dispatch.status", "routing");
      const result = await routeEmergency({
        priorityCode: state.triageResult.priorityCode,
        coords: state.location.coords,
      });
      update("dispatch.channel", result.channel);
      update("dispatch.provider", result.provider);
      update("dispatch.etaMinutes", result.etaMinutes);
      update("dispatch.status", "en-route");
      setRouting(false);

      interval = setInterval(() => {
        update("dispatch.etaMinutes", Math.max(0, state.dispatch.etaMinutes - 1));
      }, 60000);
    })();

    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [priorityCode, priorityLabel] = (state.triageResult.priorityCode || " — ").split(" — ");

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-xl font-semibold">Dispatch</h1>
        <p className="text-mist text-sm mt-1">
          {routing ? "Finding the fastest available responder…" : "Help is on the way."}
        </p>
      </div>

      <Card className="flex flex-col items-center gap-4">
        <PriorityBadge code={priorityCode} label={priorityLabel} />

        {routing ? (
          <p className="font-mono text-sm text-mist animate-pulse">Routing…</p>
        ) : (
          <>
            <div className="text-center">
              <p className="text-xs font-mono uppercase tracking-wide text-mist">
                {state.dispatch.channel === "private" ? "Private — AURA network" : "Public — 10177 fallback"}
              </p>
              <p className="font-display text-3xl mt-1">{state.dispatch.provider}</p>
            </div>
            <div className="text-center">
              <p className="text-xs font-mono uppercase tracking-wide text-mist">ETA</p>
              <p className="font-mono text-4xl text-signal-red font-semibold">
                {state.dispatch.etaMinutes} min
              </p>
            </div>
          </>
        )}
      </Card>

      {!routing && (
        <button
          onClick={() => navigate("/hospital")}
          className="rounded-lg bg-verified-teal text-ink font-medium py-2.5 text-sm"
        >
          View hospital status
        </button>
      )}
    </div>
  );
}
