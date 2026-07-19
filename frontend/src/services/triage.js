// Converts rapid tap-answers into a standard priority code. A real build
// would send { answers } to an AI triage endpoint — this mock applies
// simple rules so the demo flow is deterministic and explainable live.

export const TRIAGE_QUESTIONS = [
  { id: "conscious", label: "Is the patient conscious?" },
  { id: "breathing", label: "Is the patient breathing normally?" },
  { id: "bleeding", label: "Is there severe or uncontrolled bleeding?" },
  { id: "mobile", label: "Can the patient move or respond to voice?" },
];

export async function scorePriority(answers) {
  await wait(500);

  if (answers.breathing === "no" || answers.conscious === "no") {
    return { ok: true, code: "Code Red", label: "Respiratory / Cardiac Arrest" };
  }
  if (answers.bleeding === "yes") {
    return { ok: true, code: "Code Red", label: "Severe Haemorrhage" };
  }
  if (answers.mobile === "no") {
    return { ok: true, code: "Code Amber", label: "Serious, Stable" };
  }
  return { ok: true, code: "Code Green", label: "Minor / Stable" };
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
