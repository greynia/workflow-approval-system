import type { Preview } from "@storybook/nextjs-vite"
import { withThemeByClassName } from "@storybook/addon-themes"
import { INITIAL_VIEWPORTS } from "storybook/viewport"
import { NextIntlClientProvider } from "next-intl"
import en from "../src/i18n/messages/en.json"
import zhTW from "../src/i18n/messages/zh-TW.json"
import "../src/app/globals.css"

const messagesByLocale: Record<string, typeof en> = {
  en,
  "zh-TW": zhTW,
}

const preview: Preview = {
  globalTypes: {
    locale: {
      name: "Locale",
      description: "UI language",
      defaultValue: "zh-TW",
      toolbar: {
        icon: "globe",
        items: [
          { value: "zh-TW", title: "繁中" },
          { value: "en", title: "English" },
        ],
        showName: true,
      },
    },
  },

  decorators: [
    (Story, context) => {
      const locale =
        (context.globals.locale as string | undefined) ?? "zh-TW"
      const messages = messagesByLocale[locale] ?? messagesByLocale["zh-TW"]
      return (
        <NextIntlClientProvider locale={locale} messages={messages}>
          <Story />
        </NextIntlClientProvider>
      )
    },
    withThemeByClassName({
      themes: {
        "Indigo (Default)": "theme-indigo",
        "Warm (Orange)": "theme-warm",
        "Forest (Emerald)": "theme-forest",
        "Rose": "theme-rose",
        "Violet": "theme-violet",
      },
      defaultTheme: "Indigo (Default)",
    }),
  ],

  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    a11y: {
      test: "todo",
    },
    viewport: {
      options: INITIAL_VIEWPORTS,
    },
  },
}

export default preview
