# Feature Spec: Location Intelligence (Issue 5)

## Problem Statement
Standard GPS/street addressing fails in informal settlements like Diepsloot — there's
no street grid to route against, and dense structures cause real GPS degradation (the
same urban-canyon signal-blockage/multipath mechanism documented in
`research/device-location-accuracy.md`'s PLOS One citation). The panic button needs to
pinpoint a caller precisely enough for dispatch to actually find them, entirely offline,
computed on-device at the moment of trigger. This is experienced by the citizen at
panic-button trigger time, and downstream by the dispatcher/paramedic reading the
payload.

## Success Criteria
- [ ] `getBrowserLocation()` requests `enableHighAccuracy: true` and an explicit
      `timeout`, samples `watchPosition()` for a short bounded window, and returns the
      best (lowest-`accuracy`) reading rather than the first one
- [ ] The returned result includes the accuracy radius (meters) alongside the
      coordinate, not just `{lat, lng}`
- [ ] When the best sampled accuracy is worse than 50m, the flow falls back to the
      existing what3words entry path instead of silently trusting a bad fix
- [ ] A Plus Code is generated from the coordinate and attached to the trigger
      payload's `location.plusCode` field (currently always `null`)
- [ ] The Plus Code generation step itself produces a code with the device in airplane
      mode, after the coordinate was already acquired — proving the offline claim for
      the encoding step specifically, not conflated with coordinate acquisition
- [ ] The user sees a visible "locating you precisely…" status during the sampling
      window, not a silent multi-second delay
- [ ] The Plus Code round-trips correctly through the existing trigger payload into
      whatever currently consumes `location.plusCode` on the backend

## Scope

### In Scope
- Improving `getBrowserLocation()`'s accuracy behavior (high-accuracy hint, timeout,
  bounded sampling window, returning the accuracy figure)
- Plus Code encoding via `open-location-code` and wiring it into the panic-button
  trigger flow
- Wiring the existing `resolveWhat3Words()` mock in as the real low-accuracy fallback
  path (currently unused/dead)
- A visible sampling-status UI state
- A verification procedure proving the offline encoding claim correctly

### Out of Scope
- Indoor positioning — already dropped project-wide (ISSUES.md: no free-and-fast
  alternative exists)
- Turn-by-turn navigation for the ambulance driver — this issue produces a location,
  not directions
- Reverse-geocoding a Plus Code back into a human-readable street address
- Any backend change — `LocationPayload(String plusCode)` already exists and expects
  exactly this field; only the frontend needs to actually populate it

## Data / API Contract

`getBrowserLocation()` (frontend/src/services/location.js) — revised return shape:
```js
{ ok: true, coords: { lat, lng }, accuracy: <meters> }
// or
{ ok: false, reason: "..." }
```

New function, `generatePlusCode(coords)`:
```js
// input: { lat, lng }
// output: a Plus Code string, e.g. "8FRP+2Q", via open-location-code's encode()
```

Trigger payload (already defined by the steel thread, Issue 1) — `location.plusCode`
gets populated instead of staying `null`:
```json
{ "location": { "plusCode": "8FRP+2Q" } }
```

## Tech Stack
- Frontend: React (existing), `open-location-code` (new npm dependency, zero runtime
  dependencies itself, Apache 2.0)
- No backend changes — `backend/src/main/java/com/healthline/backend/dispatch/LocationPayload.java`
  already accepts this field
- Test runner: whatever the frontend already uses (Vitest/Testing Library, per
  `frontend/package.json`'s existing devDependencies)

## Open Questions
- Exact wording/visual treatment of the "locating you precisely…" status — an
  implementation-time UI detail, not contract-level
- **Demo-mechanics risk (not code scope, but must not be forgotten on the day):** the
  airplane-mode demo requires the app already fully loaded and interacted with once
  *before* toggling airplane mode — a mid-demo page reload would fail on the JS bundle
  itself, not because encoding needs network. Location permission should also be
  pre-granted in the demo browser beforehand, not requested live in front of judges.

## Implementation Plan
1. `frontend`: add `open-location-code` to `package.json`
2. `frontend/src/services/location.js`: revise `getBrowserLocation()` — add
   `enableHighAccuracy: true`, an explicit `timeout`, replace the single
   `getCurrentPosition()` call with a bounded `watchPosition()` sampling window that
   clears itself and resolves with the best-`accuracy` reading seen; return `accuracy`
   in the result
3. `frontend/src/services/location.js`: add `generatePlusCode(coords)` using
   `open-location-code`'s `encode()`
4. `frontend`: wherever the panic-button trigger assembles its payload, call
   `getBrowserLocation()`, check the returned `accuracy` against the 50m threshold —
   if within threshold, call `generatePlusCode()` and attach `location.plusCode`; if
   not, fall back to the existing (currently mocked) `resolveWhat3Words()` entry path
5. `frontend`: add a visible sampling-status UI state (e.g. reusing the existing
   `StatusRail` pattern already in the app) shown during the `watchPosition()` window
6. Tests: unit tests for `generatePlusCode()` (known coordinate → known Plus Code,
   matching Google's reference test vectors), tests for `getBrowserLocation()`'s
   best-of-window selection logic (mocked `watchPosition` emitting multiple readings,
   assert the lowest-accuracy one wins), and a test for the 50m-threshold fallback
   decision
7. Manual verification: with the app already loaded, enable airplane mode, trigger
   panic button, confirm a Plus Code still generates and attaches correctly
