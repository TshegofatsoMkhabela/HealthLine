import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { useApp } from "../state/AppContext";
import Card from "../components/Card";

const QR_LIFETIME_SECONDS = 60;

export default function Handoff() {
  const { state } = useApp();
  const [secondsLeft, setSecondsLeft] = useState(QR_LIFETIME_SECONDS);
  const [scanned, setScanned] = useState(false);
  const [selfDestructed, setSelfDestructed] = useState(false);

  useEffect(() => {
    if (scanned || selfDestructed) return;
    if (secondsLeft <= 0) {
      setSelfDestructed(true);
      return;
    }
    const t = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [secondsLeft, scanned, selfDestructed]);

  useEffect(() => {
    if (!scanned) return;
    const t = setTimeout(() => setSelfDestructed(true), 8000);
    return () => clearTimeout(t);
  }, [scanned]);

  const payload = JSON.stringify({
    idNumber: state.identity.idNumber,
    fullRecord: state.profile.fullRecord,
    ts: Date.now(),
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-xl font-semibold">Handoff at the hospital</h1>
        <p className="text-mist text-sm mt-1">
          Full record transfers on scan, then disappears. No hospital
          integration required.
        </p>
      </div>

      <Card className="flex flex-col items-center gap-4">
        {selfDestructed ? (
          <p className="font-mono text-sm text-mist py-10">
            Record expired and cleared from this screen.
          </p>
        ) : (
          <>
            <div className="rounded-lg bg-paper p-4 relative">
              <QRCodeSVG value={payload} size={180} />
            </div>
            <p className="font-mono text-xs text-signal-amber">
              Expires in {secondsLeft}s
            </p>
          </>
        )}

        {!scanned && !selfDestructed && (
          <button
            onClick={() => setScanned(true)}
            className="w-full rounded-lg border border-line py-2.5 text-sm hover:bg-ink transition-colors"
          >
            Simulate admitting doctor's scan
          </button>
        )}

        {scanned && !selfDestructed && (
          <p className="text-sm text-verified-teal">
            ✅ Scanned — record now visible in the admitting doctor's browser.
            Self-destructing shortly.
          </p>
        )}
      </Card>
    </div>
  );
}
