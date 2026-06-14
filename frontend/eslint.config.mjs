// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
import storybook from "eslint-plugin-storybook";

import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "public/mockServiceWorker.js",
    "next-env.d.ts",
    // Jest config files use CommonJS require() as required by Next.js docs.
    "jest.config.js",
    "jest.environment.js",
    "jest.setup.js",
    "jest.polyfills.js",
    // Storybook build output — not source, must not be linted.
    "storybook-static/**",
  ]),
  ...storybook.configs["flat/recommended"]
]);

export default eslintConfig;
