import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../state/AppContext";
import { saveProfile, loadProfile } from "../services/profile";
import Card from "../components/Card";
import PageHeader from "../components/PageHeader";

function Field({ label, value, onChange, placeholder }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-mono uppercase tracking-wide text-mist">{label}</span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="rounded-lg border border-line bg-ink px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-verified-teal"
      />
    </label>
  );
}

export default function ProfileBuilder() {
  const { state, update } = useApp();
  const navigate = useNavigate();
  const [triage, setTriage] = useState(state.profile.triage);
  const [fullRecord, setFullRecord] = useState(state.profile.fullRecord);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    loadProfile()
      .then(({ triage: savedTriage, fullRecord: savedFullRecord }) => {
        if (savedTriage) setTriage(savedTriage);
        if (savedFullRecord) setFullRecord(savedFullRecord);
      })
      .catch((error) => {
        console.error("Failed to load saved profile:", error);
        setLoadError("Couldn't load your saved profile — starting from a blank form.");
      });
  }, []);

  const setT = (key) => (val) => setTriage((prev) => ({ ...prev, [key]: val }));
  const setF = (key) => (val) => setFullRecord((prev) => ({ ...prev, [key]: val }));

  const save = async () => {
    setSaving(true);
    await saveProfile({ triage, fullRecord });
    update("profile.triage", triage);
    update("profile.fullRecord", fullRecord);
    update("profile.status", "draft");
    setSaving(false);
    navigate("/doctor-sign");
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Build your medical profile"
        subtitle="Stored encrypted, on your device only — POPIA compliant by design."
      />

      {loadError && <p className="text-sm text-signal-red">{loadError}</p>}

      <Card className="flex flex-col gap-4">
        <p className="text-xs font-mono uppercase tracking-wide text-signal-amber">
          Triage payload — sent to the ambulance
        </p>
        <Field label="Blood type" value={triage.bloodType} onChange={setT("bloodType")} placeholder="O+" />
        <Field label="Severe allergies" value={triage.allergies} onChange={setT("allergies")} placeholder="Penicillin" />
        <Field label="Critical medications" value={triage.medications} onChange={setT("medications")} placeholder="Warfarin" />
        <Field label="Major chronic conditions" value={triage.conditions} onChange={setT("conditions")} placeholder="Type 1 diabetes" />
      </Card>

      <Card className="flex flex-col gap-4">
        <p className="text-xs font-mono uppercase tracking-wide text-verified-teal">
          Full record — stays on your phone until the hospital
        </p>
        <Field label="Past surgeries" value={fullRecord.surgeries} onChange={setF("surgeries")} placeholder="Appendectomy, 2019" />
        <Field label="Family history" value={fullRecord.familyHistory} onChange={setF("familyHistory")} placeholder="Hypertension" />
        <Field label="GP / clinic" value={fullRecord.gp} onChange={setF("gp")} placeholder="Dr. Naidoo, Sasolburg Clinic" />
        <Field label="Next of kin" value={fullRecord.nextOfKin} onChange={setF("nextOfKin")} placeholder="Name + phone number" />
      </Card>

      <button
        disabled={saving}
        onClick={save}
        className="rounded-lg bg-verified-teal text-ink font-medium py-2.5 text-sm disabled:opacity-40"
      >
        {saving ? "Saving…" : "Save profile"}
      </button>
    </div>
  );
}
