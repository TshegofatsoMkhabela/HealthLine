import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { findAcceptingHospital } from "../services/dispatch";
import Card from "../components/Card";
import PageHeader from "../components/PageHeader";

export default function HospitalBoard() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);

  useEffect(() => {
    findAcceptingHospital().then(setData);
  }, []);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Hospital load-balancer"
        subtitle="Skips any ward on diversion. Ward is alerted 3km out."
      />

      <div className="flex flex-col gap-3">
        {!data && <p className="text-sm text-mist font-mono">Checking hospital status…</p>}
        {data?.hospitals.map((h) => (
          <Card
            key={h.name}
            className={`flex items-center justify-between ${
              h.status === "accepting" ? "border-verified-teal/40" : "opacity-50"
            }`}
          >
            <div>
              <p className="text-sm font-medium">{h.name}</p>
              <p className="text-xs text-mist">{h.distanceKm} km away</p>
            </div>
            <span
              className={`text-xs font-mono uppercase px-2 py-1 rounded-md ${
                h.status === "accepting"
                  ? "bg-verified-teal/15 text-verified-teal"
                  : "bg-signal-red/15 text-signal-red"
              }`}
            >
              {h.status}
            </span>
          </Card>
        ))}
      </div>

      {data && (
        <Card className="border-verified-teal/40 bg-verified-teal/5">
          <p className="text-sm">
            Routing to <span className="font-semibold">{data.selected?.name}</span>.
            Ward alert fires automatically when the ambulance is 3km out.
          </p>
        </Card>
      )}

      <button
        onClick={() => navigate("/handoff")}
        disabled={!data}
        className="rounded-lg bg-signal-red text-paper font-medium py-2.5 text-sm disabled:opacity-40"
      >
        Simulate arrival → handoff
      </button>
    </div>
  );
}
