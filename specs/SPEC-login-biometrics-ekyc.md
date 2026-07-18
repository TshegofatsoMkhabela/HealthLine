# Feature Spec: Login, Biometrics, eKYC (Issue 2 / GitHub #TBD)

## Problem Statement
Nothing currently verifies that a user of the app is who they claim to be. Without that,
the app is open to prank/abuse traffic — every other journey (panic button, dispatch,
hospital handoff) implicitly trusts that the identity behind a session is real. A user
needs to prove they're a real, DHA-registered person once at signup, then quickly
reconfirm it's still them on every later login without repeating the full check.

## Success Criteria
- [ ] A sandboxed SA ID + selfie run against Smile ID's Enhanced KYC (`job_type: 5`)
      returns a normalized match result end-to-end — or, since sandbox credentials
      aren't available yet, a clearly labeled `MOCK` verifier stands in, returning a
      structurally-identical response shape, logged as an open item
- [ ] On a successful match (real or `MOCK`), `ai-service` computes a Facenet512 face
      embedding from the enrollment selfie and stores it in a new `identity_embeddings`
      table, keyed to the verified identity
- [ ] A repeat login with the enrolled user's live camera capture is accepted (cosine
      distance under DeepFace's Facenet512 default threshold, 0.30) in about a second
- [ ] A different person's face is correctly rejected as a non-match
- [ ] Failure at either step (no Smile ID/MOCK match, no embedding match) is surfaced to
      the user, not swallowed
- [ ] Both `backend` and `ai-service` independently pass their own `/health` check, and
      an external pinger (e.g. UptimeRobot) hits both every 5 minutes to prevent
      cold-start stalls
- [ ] A live run-through of the full browser → `backend` → `ai-service` → `backend` →
      browser round trip completes without a cold-start delay at either hop

## Scope

### In Scope
- Enrollment: capture SA ID number + liveness selfie via `getUserMedia` in the bare-bones
  test harness (same harness pattern as the steel thread — no UI polish)
- `backend`: call Smile ID Enhanced KYC sandbox (`job_type: 5`), or a labeled `MOCK`
  verifier if sandbox credentials aren't available in time
- `ai-service`: compute and store a Facenet512 embedding on successful enrollment match
- Login: capture a new live selfie via an explicit "Login" button (not auto-triggered),
  `ai-service` compares it 1:1 against the stored embedding and returns a match/no-match
- Two-service warm-keeping: external health-check pinger on both `backend` and
  `ai-service`

### Out of Scope
- Production DHA verification — sandbox/test-ID only; real DHA access is a
  post-hackathon dependency (per the issue's own scope)
- 1:N face identification — only 1:1 recheck against the one enrolled user's own stored
  embedding
- Any biometric modality beyond face — no fingerprint, voice, or other factor
- HPCSA practice-number validation — that's Issue 4, a different verification entirely
- Real anti-spoofing / liveness-challenge modeling at login — accepted, documented risk;
  see Tech Stack decision below
- Custom threshold tuning — no labeled validation dataset exists in this timeline; use
  DeepFace's framework-default Facenet512 threshold as-is

## Data / API Contract

### 1. Enroll (signup-time deep check)
`POST /api/identity/enroll`

Request:
```json
{
  "idNumber": "0000000000000",
  "selfieBlob": "<base64 JPEG from getUserMedia capture>"
}
```

Response (success):
```json
{
  "identityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "verified",
  "verifiedAt": "2026-07-18T14:32:05Z",
  "verifier": "MOCK"
}
```
`verifier` is either `"smile_id"` or `"MOCK"` — always present, never silently omitted,
per the project's MOCK-labeling discipline.

Response (failure):
```json
{ "status": "rejected", "reason": "ID/selfie did not match DHA records." }
```

Internally: `backend` calls Smile ID (or `MOCK`) → on match, `backend` calls
`ai-service`'s `POST /identity/embed` with the same selfie → embedding stored in
`identity_embeddings`, keyed by `identityId`.

### 2. Login recheck (fast, repeat check)
`POST /api/identity/recheck`

Request:
```json
{
  "identityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "selfieBlob": "<base64 JPEG from live getUserMedia capture>"
}
```

Response:
```json
{ "identityId": "...", "match": true, "distance": 0.21 }
```
or
```json
{ "identityId": "...", "match": false, "distance": 0.58 }
```

Internally: `backend` forwards to `ai-service`'s `POST /identity/compare`, which loads
the stored embedding, computes cosine distance against a fresh embedding of the new
selfie, and returns match/no-match against the 0.30 default threshold.

### 3. `ai-service` internal endpoints
`POST /identity/embed` — `{ identityId, selfieBlob }` → `{ ok: true }` (stores embedding)
`POST /identity/compare` — `{ identityId, selfieBlob }` → `{ match: bool, distance: float }`

## Tech Stack
- Backend: Java 21, Spring Boot, Maven, JUnit 5 + AssertJ, reusing
  `AbstractHealthIntegrationTest`'s pattern for new integration tests
- AI service: Python, FastAPI, pytest, adding `deepface` to `requirements.txt`
- Face model: **Facenet512** (chosen over ArcFace — inside DeepFace's actual packaged
  framework, Facenet512 measures ~97.4% vs ArcFace's ~87.8%; ArcFace's famous 99.4% is a
  paper-only number under different training/eval conditions), with DeepFace's
  **framework-default cosine threshold (0.30)** — not custom-tuned, since no labeled
  validation dataset exists in this timeline. See `research/deepface-login-recheck.md`.
- Anti-spoofing: **accepted risk, not built.** Login recheck requires a live
  `getUserMedia` video frame (not a file upload) as a cheap partial mitigation against
  static-photo replay, but this is explicitly *not* equivalent to Smile ID's
  enrollment-time liveness (dynamic challenge-response, ISO 30107-3 Level 2). Documented
  here rather than silently presented as fully secure, consistent with the project's
  MOCK-labeling honesty discipline.
- Embedding storage: new `identity_embeddings` table in `backend`'s existing DB, keyed
  by `identityId` — physically separate from other identity fields, per the same
  "structural separation over filtering later" pattern used for Issue 3's Triage/Full-EHR
  split
- Deployment: CPU-only (Render free tier) — confirmed adequate by production precedent
  (DeepFace at 1M+/month validation volume ran faster/more stable on CPU than GPU)
- Smile ID integration: real Enhanced KYC sandbox call if credentials are available in
  time, otherwise a clearly labeled `MOCK` verifier with a structurally-identical
  response shape, logged as an open item — matching Issue 1's MOCK-labeling precedent

## Open Questions
- Smile ID sandbox credential availability/provisioning timeline — outside this project's
  control; if they arrive mid-build, swap the `MOCK` verifier for the real call without
  changing the response contract
- Whether Facenet512's model weights meaningfully worsen Render free-tier cold-start time
  beyond what the existing warm-keeping pinger already accounts for — not measured yet,
  worth a quick timing check once `ai-service` loads the model for the first time
- Exact getUserMedia capture UX (single frame vs. short burst) — implementation-time
  detail, not contract-level
- `/api/identity/recheck` has no session/auth binding beyond possession of `identityId` —
  acceptable for the current MOCK phase (security review, 2026-07-19: two related findings
  filtered out as hardening-not-vulnerability against a system with no stated security
  boundary yet), but re-evaluate before `IdentityVerifier` moves off `MOCK`: at that point
  `identityId` starts guarding a real person's biometric data, and (a) recheck returning the
  raw cosine `distance` rather than only `match: true/false`, and (b) recheck accepting any
  caller who holds a leaked `identityId` with no additional auth factor, both become real
  gaps worth closing rather than accepted scope.

## Implementation Plan
1. `backend`: add `IdentityController` (`POST /api/identity/enroll`,
   `POST /api/identity/recheck`) — new file
2. `backend`: add `SmileIdClient` (real sandbox call) and `MockSmileIdVerifier`, selected
   via a config flag, both implementing a common `IdentityVerifier` interface — labeled
   `MOCK` in logs/config when the mock path is active
3. `backend`: add `identity_embeddings` table/entity + repository, keyed by `identityId`
4. `backend`: add `AiServiceClient` methods for `POST /identity/embed` and
   `POST /identity/compare`, reusing the steel thread's existing REST client pattern
5. `ai-service`: add `deepface` to `requirements.txt`; load the Facenet512 model once at
   startup (module-level, not per-request)
6. `ai-service`: add `POST /identity/embed` (compute + return embedding for `backend` to
   store) and `POST /identity/compare` (compute new embedding, compare to one `backend`
   sends over, return match/distance) — keep embedding storage itself in `backend`'s DB,
   not duplicated in `ai-service`, per the steel-thread precedent of `backend` owning
   state
7. Test harness: add SA ID input + `getUserMedia` capture for enrollment, and a separate
   explicit "Login" button that captures a fresh frame and calls `/recheck` — bare-bones,
   no styling, matching Issue 1's harness precedent
8. Warm-keeping: document the external pinger setup (UptimeRobot or equivalent) hitting
   both `backend` and `ai-service` `/health` every 5 minutes — an infra/ops step, not
   application code
9. Tests: per-service unit tests for `IdentityVerifier` (both real-shape and MOCK),
   `ai-service`'s embed/compare logic (same-person match, different-person reject), and
   one integration test proving the full enroll → embed → recheck → match chain,
   reusing `AbstractHealthIntegrationTest`'s pattern
