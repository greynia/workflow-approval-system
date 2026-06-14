const nextJest = require("next/jest");

const createJestConfig = nextJest({ dir: "./" });

const customConfig = {
  testEnvironment: "<rootDir>/jest.environment.js",
  testEnvironmentOptions: {
    customExportConditions: ["node", "node-addons"],
  },
  setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
  testPathIgnorePatterns: [
    "<rootDir>/node_modules/",
    "<rootDir>/.next/",
    "<rootDir>/src/__tests__/test-utils.tsx",
  ],
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
    "^next-intl$": "<rootDir>/src/__mocks__/next-intl.ts",
    "^next-intl/routing$": "<rootDir>/src/__mocks__/next-intl.ts",
    "^next-intl/navigation$": "<rootDir>/src/__mocks__/next-intl.ts",
    "^next-intl/server$": "<rootDir>/src/__mocks__/next-intl.ts",
  },
};

// createJestConfig merges transformIgnorePatterns by appending, but Jest treats
// each pattern as "OR" — a file is skipped if ANY pattern matches. So we must
// replace the array AFTER resolution to ensure MSW's pure-ESM deps are transformed.
module.exports = async () => {
  const config = await createJestConfig(customConfig)();
  config.transformIgnorePatterns = [
    // Allow msw and all its ESM-only dependencies to be transformed by SWC
    "/node_modules/(?!(msw|@mswjs|rettime|until-async|outvariant|strict-event-emitter|@open-draft|headers-polyfill|is-node-process|path-to-regexp)/)",
    // Keep Next.js CSS module exclusion
    "^.+\\.module\\.(css|sass|scss)$",
  ];
  return config;
};
