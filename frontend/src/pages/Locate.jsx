import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Map, { Marker } from "react-map-gl/mapbox";
import { useApp } from "../state/AppContext";
import {
  resolveWhat3Words,
  getBrowserLocation,
  getIndoorPosition,
  generatePlusCode,
  isAccurateEnough,
} from "../services/location";
import Card from "../components/Card";
import PageHeader from "../components/PageHeader";

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN;

export default function Locate() {
  const { state, update } = useApp();
  const navigate = useNavigate();
  const [w3w, setW3w] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const coords = state.location.coords;

  const lockGps = async () => {
    setBusy(true);
    setError(null);
    update("location.status", "locating");
    const result = await getBrowserLocation();
    if (!result.ok) {
      setError(result.reason);
      update("location.status", "idle");
      setBusy(false);
      return;
    }
    if (!isAccurateEnough(result.accuracy)) {
      setError(
        `GPS fix is only accurate to ${Math.round(result.accuracy)}m — try what3words instead for a precise location.`,
      );
      update("location.accuracy", result.accuracy);
      update("location.status", "idle");
      setBusy(false);
      return;
    }
    update("location.coords", result.coords);
    update("location.accuracy", result.accuracy);
    update("location.plusCode", generatePlusCode(result.coords));
    update("location.status", "locked");
    setBusy(false);
  };

  const lockWhat3Words = async () => {
    setBusy(true);
    update("location.status", "locating");
    const result = await resolveWhat3Words(w3w);
    if (result.ok) {
      update("location.what3words", result.words);
      update("location.coords", result.coords);
      update("location.status", "locked");
    } else {
      setError(result.reason);
      update("location.status", "idle");
    }
    setBusy(false);
  };

  const toggleIndoor = async () => {
    if (state.location.indoorMode) {
      update("location.indoorMode", false);
      return;
    }
    update("location.indoorMode", true);
    const result = await getIndoorPosition();
    if (result.ok) update("location.indoorPosition", result);
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Locating you"
        subtitle="GPS first. what3words or indoor positioning if that fails."
      />

      <Card className="h-56 overflow-hidden p-0">
        {state.location.indoorMode ? (
          <div className="h-full w-full flex flex-col items-center justify-center gap-2 bg-panel">
            <p className="font-mono text-sm text-verified-teal">Indoor position</p>
            {state.location.indoorPosition ? (
              <p className="text-lg font-display">
                {state.location.indoorPosition.floor} floor —{" "}
                {state.location.indoorPosition.room}
              </p>
            ) : (
              <p className="text-sm text-mist">Reading Wi-Fi signals…</p>
            )}
          </div>
        ) : MAPBOX_TOKEN ? (
          <Map
            mapboxAccessToken={MAPBOX_TOKEN}
            initialViewState={{
              longitude: coords?.lng ?? 28.0567,
              latitude: coords?.lat ?? -26.1076,
              zoom: 13,
            }}
            style={{ width: "100%", height: "100%" }}
            mapStyle="mapbox://styles/mapbox/dark-v11"
          >
            {coords && <Marker longitude={coords.lng} latitude={coords.lat} color="#e14b3d" />}
          </Map>
        ) : (
          <div className="h-full w-full flex items-center justify-center text-center px-4">
            <p className="text-xs text-mist">
              Add VITE_MAPBOX_TOKEN to .env to render the live map.
              {coords && (
                <>
                  <br />
                  Locked coords: {coords.lat.toFixed(4)}, {coords.lng.toFixed(4)}
                </>
              )}
            </p>
          </div>
        )}
      </Card>

      <Card className="flex flex-col gap-3">
        <button
          onClick={lockGps}
          disabled={busy}
          className="rounded-lg border border-line py-2.5 text-sm hover:bg-ink transition-colors disabled:opacity-40"
        >
          Use GPS
        </button>
        {state.location.status === "locating" && (
          <p className="text-xs text-mist font-mono animate-pulse">Locating you precisely…</p>
        )}

        <div className="flex gap-2">
          <input
            value={w3w}
            onChange={(e) => setW3w(e.target.value)}
            placeholder="filled.count.soap"
            className="flex-1 rounded-lg border border-line bg-ink px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-verified-teal"
          />
          <button
            onClick={lockWhat3Words}
            disabled={busy}
            className="rounded-lg border border-line px-4 text-sm hover:bg-ink transition-colors disabled:opacity-40"
          >
            Go
          </button>
        </div>

        <button
          onClick={toggleIndoor}
          className={`rounded-lg py-2.5 text-sm border transition-colors ${
            state.location.indoorMode
              ? "border-verified-teal text-verified-teal bg-verified-teal/10"
              : "border-line hover:bg-ink"
          }`}
        >
          {state.location.indoorMode ? "Indoor mode on" : "GPS unavailable — switch to indoor mode"}
        </button>

        {error && <p className="text-sm text-signal-red">{error}</p>}
      </Card>

      <button
        disabled={state.location.status !== "locked" && !state.location.indoorPosition}
        onClick={() => navigate("/triage")}
        className="rounded-lg bg-signal-red text-paper font-medium py-2.5 text-sm disabled:opacity-40"
      >
        Continue to triage
      </button>
    </div>
  );
}
