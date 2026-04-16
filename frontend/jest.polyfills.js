// MSW v2 requires fetch globals to be available in Jest's jsdom environment.
// Node.js 18+ has these built in, but Jest's jsdom context doesn't inherit them.
// This file runs via `setupFiles` BEFORE the test framework loads.
const { fetch, Headers, FormData, Request, Response } = globalThis;

Object.defineProperties(globalThis, {
  fetch: { value: fetch, writable: true, configurable: true },
  Headers: { value: Headers, writable: true, configurable: true },
  FormData: { value: FormData, writable: true, configurable: true },
  Request: { value: Request, writable: true, configurable: true },
  Response: { value: Response, writable: true, configurable: true },
});
