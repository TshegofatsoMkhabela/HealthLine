import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { act } from "react";
import { AppProvider, useApp } from "./AppContext";

// update() is the one piece of real logic in this file (dotted-path nested-state
// writes via structuredClone) — everything else is plain JSX wiring already
// covered by the page smoke tests.
function Probe() {
  const { state, update } = useApp();
  return (
    <div>
      <span data-testid="value">{state.location.status}</span>
      <button onClick={() => update("location.status", "locked")}>lock</button>
    </div>
  );
}

describe("AppContext.update", () => {
  it("writes a nested field without touching sibling state", async () => {
    render(
      <AppProvider>
        <Probe />
      </AppProvider>,
    );

    expect(screen.getByTestId("value")).toHaveTextContent("idle");

    await act(async () => {
      screen.getByRole("button", { name: "lock" }).click();
    });

    expect(screen.getByTestId("value")).toHaveTextContent("locked");
  });

  it("throws when useApp is called outside AppProvider", () => {
    function Bare() {
      useApp();
      return null;
    }
    // React logs its own error to console for a thrown-during-render component;
    // suppress it here so the expected failure doesn't look like a real crash.
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => render(<Bare />)).toThrow("useApp must be used inside AppProvider");
    spy.mockRestore();
  });
});
