// Custom Jest environment: inherits jsdom (for React DOM testing), but
// re-injects Node.js native fetch globals after jsdom initializes.
// Needed for MSW v2, which requires Response/Request/Headers in the test context.
const JsdomEnvironment = require("jest-environment-jsdom").default;

class CustomJsdomEnvironment extends JsdomEnvironment {
  async setup() {
    await super.setup();
    // `globalThis` here refers to the Jest runner's Node.js global (has fetch).
    // `this.global` is the test sandbox global (jsdom may have cleared fetch).
    const { TextEncoder, TextDecoder } = require("util");
    const {
      ReadableStream,
      WritableStream,
      TransformStream,
    } = require("node:stream/web");
    Object.assign(this.global, {
      fetch: globalThis.fetch,
      Headers: globalThis.Headers,
      FormData: globalThis.FormData,
      Request: globalThis.Request,
      Response: globalThis.Response,
      TextEncoder,
      TextDecoder,
      ReadableStream,
      WritableStream,
      TransformStream,
      BroadcastChannel: globalThis.BroadcastChannel,
      structuredClone: globalThis.structuredClone,
    });
  }
}

module.exports = CustomJsdomEnvironment;
