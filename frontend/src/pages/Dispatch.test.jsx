import { describe, it, expect, vi } from "vitest";
import { render, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AppProvider } from "../state/AppContext";
import Dispatch from "./Dispatch";
import * as dispatchService from "../services/dispatch";

// Regression guard for the POPIA data-minimization requirement (Issue 3): the
// dispatch call must never be able to carry Full EHR data (surgeries, family
// history, GP, next of kin), no matter what's stored in AppContext. Proven by
// inspecting the actual arguments routeEmergency() is called with, not by
// reading the source and trusting it.
const FULL_EHR_FIELD_NAMES = ["surgeries", "familyHistory", "gp", "nextOfKin"];

describe("Dispatch page", () => {
  it("calls routeEmergency with no Full EHR field present in its arguments", async () => {
    const routeEmergencySpy = vi
      .spyOn(dispatchService, "routeEmergency")
      .mockResolvedValue({ ok: true, channel: "private", provider: "AURA", etaMinutes: 8 });

    render(
      <MemoryRouter>
        <AppProvider>
          <Dispatch />
        </AppProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(routeEmergencySpy).toHaveBeenCalled());

    const callArgs = routeEmergencySpy.mock.calls[0][0];
    const flatKeys = JSON.stringify(callArgs);

    for (const field of FULL_EHR_FIELD_NAMES) {
      expect(flatKeys).not.toContain(field);
    }

    routeEmergencySpy.mockRestore();
  });
});
