import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../state/AppContext";
import { TRIAGE_QUESTIONS, scorePriority } from "../services/triage";
import PriorityBadge from "../components/PriorityBadge";
import Card from "../components/Card";
import PageHeader from "../components/PageHeader";

export default function Triage() {
  const { update } = useApp();
  const navigate = useNavigate();
  const [answers, setAnswers] = useState({});
  const [result, setResult] = useState(null);
  const [scoring, setScoring] = useState(false);

  const answer = async (id, value) => {
    const next = { ...answers, [id]: value };
    setAnswers(next);

    if (Object.keys(next).length === TRIAGE_QUESTIONS.length) {
      setScoring(true);
      const scored = await scorePriority(next);
      setResult(scored);
      update("triageResult.answers", next);
      update("triageResult.priorityCode", `${scored.code} — ${scored.label}`);
      setScoring(false);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Quick triage" subtitle="Tap answers. No typing." />

      <div className="flex flex-col gap-3">
        {TRIAGE_QUESTIONS.map((q) => (
          <Card key={q.id} className="flex items-center justify-between">
            <span className="text-sm">{q.label}</span>
            <div className="flex gap-2">
              {["yes", "no"].map((v) => (
                <button
                  key={v}
                  onClick={() => answer(q.id, v)}
                  className={`rounded-lg px-3 py-1.5 text-xs font-mono uppercase border transition-colors ${
                    answers[q.id] === v
                      ? "border-verified-teal text-verified-teal bg-verified-teal/10"
                      : "border-line hover:bg-ink"
                  }`}
                >
                  {v}
                </button>
              ))}
            </div>
          </Card>
        ))}
      </div>

      {scoring && <p className="text-sm text-mist font-mono">Scoring priority…</p>}

      {result && (
        <Card className="flex flex-col items-center gap-3">
          <PriorityBadge code={result.code} label={result.label} />
          <button
            onClick={() => navigate("/dispatch")}
            className="w-full rounded-lg bg-signal-red text-paper font-medium py-2.5 text-sm"
          >
            Dispatch now
          </button>
        </Card>
      )}
    </div>
  );
}
