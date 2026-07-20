import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AppProvider } from "../state/AppContext";
import { saveProfile } from "../services/profile";
import { deleteDb } from "../test/indexedDbTestUtils";
import ProfileBuilder from "./ProfileBuilder";

describe("ProfileBuilder", () => {
  beforeEach(async () => {
    await deleteDb("medical-passport");
  });

  it("repopulates the form with a previously saved profile on mount", async () => {
    await saveProfile({
      triage: { bloodType: "AB-", allergies: "", medications: "", conditions: "" },
      fullRecord: { surgeries: "Appendectomy", familyHistory: "", gp: "", nextOfKin: "" },
    });

    render(
      <MemoryRouter>
        <AppProvider>
          <ProfileBuilder />
        </AppProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByPlaceholderText("O+")).toHaveValue("AB-"));
    expect(screen.getByPlaceholderText("Appendectomy, 2019")).toHaveValue("Appendectomy");
  });

  it("leaves the form at its empty defaults when nothing has been saved yet", async () => {
    render(
      <MemoryRouter>
        <AppProvider>
          <ProfileBuilder />
        </AppProvider>
      </MemoryRouter>,
    );

    expect(screen.getByPlaceholderText("O+")).toHaveValue("");
  });
});
