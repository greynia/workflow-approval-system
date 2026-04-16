"use client";

import { MockServiceWorkerProvider } from "@/components/providers/mock-service-worker-provider";
import { QueryProvider } from "@/components/providers/query-provider";
import { Toaster } from "@/components/ui/toaster";

export function RootProviders({ children }: { children: React.ReactNode }) {
  return (
    <MockServiceWorkerProvider>
      <QueryProvider>
        {children}
      </QueryProvider>
      <Toaster />
    </MockServiceWorkerProvider>
  );
}
