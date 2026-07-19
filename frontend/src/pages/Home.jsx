import { Link, useNavigate } from "react-router-dom";
import { useApp } from "../state/AppContext";
import Card from "../components/Card";
import PageHeader from "../components/PageHeader";

export default function Home() {
  const { state } = useApp();
  const navigate = useNavigate();
  const isSetUp = state.profile.status !== "none";

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Medical Passport"
        subtitle="Verified identity. Paramedic-ready record. One button."
        size="2xl"
      />

      {!isSetUp && (
        <Card className="border-signal-amber/40 bg-signal-amber/5">
          <p className="text-sm">
            Your medical profile isn't set up yet. Set it up now so responders
            have what they need before you ever need the panic button.
          </p>
          <Link
            to="/onboarding"
            className="mt-3 inline-block text-sm font-medium text-signal-amber underline underline-offset-4"
          >
            Set up profile →
          </Link>
        </Card>
      )}

      <div className="flex flex-col items-center gap-3 py-8">
        <button
          onClick={() => navigate("/locate")}
          className="h-40 w-40 rounded-full bg-signal-red text-paper font-display text-xl font-semibold shadow-[0_0_0_8px_rgba(225,75,61,0.12)] active:scale-95 transition-transform"
        >
          PANIC
        </button>
        <p className="text-xs text-mist font-mono uppercase tracking-wide">
          Press and hold in a real emergency
        </p>
      </div>

      <Card>
        <p className="text-xs text-mist font-mono uppercase tracking-wide mb-2">
          Quick links
        </p>
        <div className="flex flex-col gap-2 text-sm">
          <Link className="text-verified-teal underline underline-offset-4" to="/profile">
            Edit medical profile
          </Link>
          <Link className="text-verified-teal underline underline-offset-4" to="/doctor-sign">
            Get clinically verified
          </Link>
        </div>
      </Card>
    </div>
  );
}
