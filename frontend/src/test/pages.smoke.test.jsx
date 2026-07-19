import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AppProvider } from "../state/AppContext";

import Home from "../pages/Home";
import Onboarding from "../pages/Onboarding";
import ProfileBuilder from "../pages/ProfileBuilder";
import DoctorSign from "../pages/DoctorSign";
import Locate from "../pages/Locate";
import Triage from "../pages/Triage";
import Dispatch from "../pages/Dispatch";
import HospitalBoard from "../pages/HospitalBoard";
import Handoff from "../pages/Handoff";

// One smoke test per screen: renders without throwing, with the app's default
// (nothing-set-up-yet) state, and shows the heading that identifies the screen.
// Not exhaustive coverage of each screen's interactions — those belong in
// screen-specific tests as they gain real logic worth locking down.
const screens = [
  { name: "Home", Component: Home, heading: "Medical Passport" },
  { name: "Onboarding", Component: Onboarding, heading: "Verify your identity" },
  { name: "ProfileBuilder", Component: ProfileBuilder, heading: "Build your medical profile" },
  { name: "DoctorSign", Component: DoctorSign, heading: "Get clinically verified" },
  { name: "Locate", Component: Locate, heading: "Locating you" },
  { name: "Triage", Component: Triage, heading: "Quick triage" },
  { name: "Dispatch", Component: Dispatch, heading: "Dispatch" },
  { name: "HospitalBoard", Component: HospitalBoard, heading: "Hospital load-balancer" },
  { name: "Handoff", Component: Handoff, heading: "Handoff at the hospital" },
];

describe("every screen renders with the app's default state", () => {
  it.each(screens)("$name renders its heading", ({ Component, heading }) => {
    render(
      <MemoryRouter>
        <AppProvider>
          <Component />
        </AppProvider>
      </MemoryRouter>,
    );
    expect(screen.getByRole("heading", { name: heading })).toBeInTheDocument();
  });
});
