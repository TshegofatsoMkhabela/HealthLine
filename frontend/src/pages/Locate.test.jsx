import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AppProvider, useApp } from "../state/AppContext";
import Locate from "./Locate";
import { getBrowserLocation } from "../services/location";

vi.mock("../services/location", async (importOriginal) => {
  // isAccurateEnough is pure (no I/O) — use the real implementation so this
  // test suite can't silently drift out of sync with the actual threshold
  // logic in location.js. Only the I/O-touching functions get mocked.
  const actual = await importOriginal();
  return {
    getBrowserLocation: vi.fn(),
    resolveWhat3Words: vi.fn(),
    getIndoorPosition: vi.fn(),
    generatePlusCode: vi.fn(() => "8FRP+2Q"),
    isAccurateEnough: actual.isAccurateEnough,
  };
});

function StateProbe() {
  const { state } = useApp();
  return (
    <div data-testid="state-probe">
      {JSON.stringify({
        status: state.location.status,
        plusCode: state.location.plusCode,
      })}
    </div>
  );
}

function renderLocate() {
  render(
    <MemoryRouter>
      <AppProvider>
        <Locate />
        <StateProbe />
      </AppProvider>
    </MemoryRouter>,
  );
}

function readProbe() {
  return JSON.parse(screen.getByTestId("state-probe").textContent);
}

async function renderAndClickUseGps() {
  renderLocate();
  await userEvent.click(screen.getByRole("button", { name: "Use GPS" }));
}

describe("Locate GPS lock", () => {
  it("stores coords and a Plus Code when accuracy is within the 50m threshold", async () => {
    getBrowserLocation.mockResolvedValue({
      ok: true,
      coords: { lat: -26.1076, lng: 28.0567 },
      accuracy: 15,
    });

    await renderAndClickUseGps();

    await waitFor(() => expect(readProbe().status).toBe("locked"));
    expect(readProbe().plusCode).toBe("8FRP+2Q");
  });

  it("does not lock or generate a Plus Code when accuracy is worse than 50m", async () => {
    getBrowserLocation.mockResolvedValue({
      ok: true,
      coords: { lat: -26.1076, lng: 28.0567 },
      accuracy: 120,
    });

    await renderAndClickUseGps();

    await waitFor(() =>
      expect(screen.getByText(/try what3words instead/i)).toBeInTheDocument(),
    );
    expect(readProbe().status).not.toBe("locked");
    expect(readProbe().plusCode).toBeNull();
  });

  it("shows the existing error and no Plus Code when the location request fails", async () => {
    getBrowserLocation.mockResolvedValue({ ok: false, reason: "Location permission denied." });

    await renderAndClickUseGps();

    await waitFor(() =>
      expect(screen.getByText("Location permission denied.")).toBeInTheDocument(),
    );
    expect(readProbe().plusCode).toBeNull();
  });

  it("shows a locating-precisely status while the request is in flight", async () => {
    let resolveLocation;
    getBrowserLocation.mockReturnValue(
      new Promise((resolve) => {
        resolveLocation = resolve;
      }),
    );

    await renderAndClickUseGps();

    expect(screen.getByText(/locating you precisely/i)).toBeInTheDocument();

    resolveLocation({ ok: true, coords: { lat: -26.1076, lng: 28.0567 }, accuracy: 15 });
  });
});
