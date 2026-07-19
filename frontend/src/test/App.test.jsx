import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "../App";

// App itself owns the real BrowserRouter + AppProvider wiring (not swapped out here) —
// this is the one test proving that wiring actually works end to end, not just that
// each page renders in isolation under a test-only MemoryRouter.
describe("App", () => {
  it("boots at the panic button and can navigate into the flow", async () => {
    const user = userEvent.setup();
    render(<App />);

    expect(screen.getByRole("heading", { name: "Medical Passport" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /panic/i })).toBeInTheDocument();

    await user.click(screen.getByRole("link", { name: /set up profile/i }));

    expect(
      screen.getByRole("heading", { name: "Verify your identity" }),
    ).toBeInTheDocument();
  });
});
