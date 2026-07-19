import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../state/AppContext";
import { verifyIdentity } from "../services/identity";
import Card from "../components/Card";

export default function Onboarding() {
  const { update } = useApp();
  const navigate = useNavigate();
  const videoRef = useRef(null);
  const [idNumber, setIdNumber] = useState("");
  const [cameraOn, setCameraOn] = useState(false);
  const [status, setStatus] = useState("idle"); // idle | verifying | error
  const [error, setError] = useState(null);

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      if (videoRef.current) videoRef.current.srcObject = stream;
      setCameraOn(true);
    } catch {
      setError("Camera access denied — you can still continue for the demo.");
    }
  };

  const submit = async () => {
    setStatus("verifying");
    update("identity.status", "pending");
    setError(null);

    const result = await verifyIdentity({ idNumber, selfieBlob: null });

    if (!result.ok) {
      setStatus("error");
      setError(result.reason);
      update("identity.status", "unverified");
      return;
    }

    update("identity.status", "verified");
    update("identity.idNumber", idNumber);
    navigate("/profile");
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-xl font-semibold">Verify your identity</h1>
        <p className="text-mist text-sm mt-1">
          Checked against the DHA registry via Smile ID. Done once, ever.
        </p>
      </div>

      <Card className="flex flex-col gap-4">
        <label className="flex flex-col gap-1.5">
          <span className="text-xs font-mono uppercase tracking-wide text-mist">
            13-digit ID number
          </span>
          <input
            value={idNumber}
            onChange={(e) => setIdNumber(e.target.value.replace(/\D/g, "").slice(0, 13))}
            placeholder="0000000000000"
            className="rounded-lg border border-line bg-ink px-3 py-2 font-mono text-sm tracking-wider focus:outline-none focus:ring-2 focus:ring-verified-teal"
          />
        </label>

        <div className="flex flex-col gap-2">
          <span className="text-xs font-mono uppercase tracking-wide text-mist">
            Liveness selfie
          </span>
          {!cameraOn ? (
            <button
              onClick={startCamera}
              className="rounded-lg border border-line py-2 text-sm hover:bg-ink transition-colors"
            >
              Enable camera
            </button>
          ) : (
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full rounded-lg border border-line aspect-video object-cover"
            />
          )}
        </div>

        {error && <p className="text-sm text-signal-red">{error}</p>}

        <button
          disabled={idNumber.length !== 13 || status === "verifying"}
          onClick={submit}
          className="rounded-lg bg-verified-teal text-ink font-medium py-2.5 text-sm disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {status === "verifying" ? "Verifying with DHA…" : "Verify identity"}
        </button>
      </Card>
    </div>
  );
}
