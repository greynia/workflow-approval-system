import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "next-intl";
import { render, type RenderOptions } from "@testing-library/react";
import type { ReactElement } from "react";
import enMessages from "@/i18n/messages/en.json";
import { Toaster } from "@/components/ui/toaster";

export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, "wrapper">
) {
  const queryClient = createTestQueryClient();
  const renderResult = render(
    <NextIntlClientProvider locale="en" messages={enMessages}>
      <QueryClientProvider client={queryClient}>
        {ui}
        <Toaster />
      </QueryClientProvider>
    </NextIntlClientProvider>,
    options
  );

  return {
    ...renderResult,
    queryClient,
  };
}
