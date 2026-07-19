import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { useApp } from "../state/AppContext";
import { requestDoctorSignOff } from "../services/profile";
import Card from "../components/Card";
import PageHeader from "../components/PageHeader";

export default function DoctorSign() {
  const { state, update } = useApp();
  const [hpcsaNumber, setHpcsaNumber] = useState("");
  const [status, setStatus] = useState("idle"); // idle | signing | error
  const [error, setError] = useState(null);

  const isSigned = state.profile.status === "clinically-verified";

  const qrPayload = JSON.stringify({
    idNumber: state.identity.idNumber,
    triage: state.profile.triage,
    ts: Date.now(),
  });

  const simulateGpScan = async () => {
    setStatus("signing");
    setError(null);
    const result = await requestDoctorSignOff({ hpcsaNumber });
    if (!result.ok) {
      setStatus("error");
      setError(result.reason);
      return;
    }
    update("profile.status", "clinically-verified");
    setStatus("idle");
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Get clinically verified"
        subtitle={
          <>
            Optional, but a doctor's signature gives paramedics a green checkmark
            they can treat on without hesitation.
          </>
        }
      />

      <Card className="flex flex-col items-center gap-4">
        <div className="rounded-lg bg-paper p-4">
          <QRCodeSVG value={qrPayload} size={180} />
        </div>
        <p className="text-xs text-mist text-center">
          Show this at your GP or clinic. This screen simulates the doctor's
          side of the scan — swap for a real scanner (e.g. html5-qrcode) later.
        </p>
      </Card>

      {isSigned ? (
        <Card className="border-verified-teal/40 bg-verified-teal/5">
          <p className="text-sm text-verified-teal font-medium">✅ Clinically verified</p>
          <p className="text-xs text-mist mt-1">{state.identity.idNumber ? "Signed and stored on-device." : ""}</p>
        </Card>
      ) : (
        <Card className="flex flex-col gap-3">
          <p className="text-xs font-mono uppercase tracking-wide text-mist">
            Doctor's side — enter HPCSA number to sign
          </p>
          <input
            value={hpcsaNumber}
            onChange={(e) => setHpcsaNumber(e.target.value)}
            placeholder="HPCSA registration number"
            className="rounded-lg border border-line bg-ink px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-verified-teal"
          />
          {error && <p className="text-sm text-signal-red">{error}</p>}
          <button
            onClick={simulateGpScan}
            disabled={status === "signing"}
            className="rounded-lg bg-verified-teal text-ink font-medium py-2.5 text-sm disabled:opacity-40"
          >
            {status === "signing" ? "Verifying HPCSA…" : "Sign profile"}
          </button>
        </Card>
      )}
    </div>
  );
}
