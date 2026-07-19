const CODE_STYLES = {
  "Code Red": "bg-signal-red/15 text-signal-red border-signal-red/40",
  "Code Amber": "bg-signal-amber/15 text-signal-amber border-signal-amber/40",
  "Code Green": "bg-verified-teal/15 text-verified-teal border-verified-teal/40",
};

export default function PriorityBadge({ code, label }) {
  const style = CODE_STYLES[code] || "bg-line/40 text-mist border-line";
  return (
    <div className={`inline-flex flex-col gap-0.5 rounded-lg border px-3 py-1.5 ${style}`}>
      <span className="font-mono text-sm font-semibold tracking-wide">{code}</span>
      {label && <span className="text-xs text-mist">{label}</span>}
    </div>
  );
}
