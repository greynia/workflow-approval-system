import path from "path"
import { fileURLToPath } from "url"
import type { StorybookConfig } from '@storybook/nextjs-vite';

const __dirname = fileURLToPath(new URL(".", import.meta.url))

const config: StorybookConfig = {
  "stories": [
    "../src/**/*.mdx",
    "../src/**/*.stories.@(js|jsx|mjs|ts|tsx)"
  ],
  "addons": [
    "@chromatic-com/storybook",
    "@storybook/addon-vitest",
    "@storybook/addon-a11y",
    "@storybook/addon-docs",
    "@storybook/addon-onboarding",
    "@storybook/addon-themes",
    "storybook-addon-pseudo-states",
    "storybook/viewport"
  ],
  "framework": "@storybook/nextjs-vite",
  "staticDirs": [
    "../public"
  ],
  viteFinal: async (config) => {
    config.resolve = config.resolve ?? {}
    config.resolve.alias = {
      ...config.resolve.alias,
      "@/i18n/navigation": path.resolve(__dirname, "../src/i18n/navigation.mock.ts"),
    }
    return config
  },
};
export default config;