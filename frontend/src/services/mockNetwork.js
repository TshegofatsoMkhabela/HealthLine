// Shared by every mocked service in this directory to simulate real network
// latency. Swap an individual service's body for a real fetch() and this
// import goes away for that file — nothing else needs to change.
export function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
