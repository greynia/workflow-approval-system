"use client";

import { useEffect, useState } from "react";
import { isMockApiEnabled } from "@/lib/mock-api";

let workerStartPromise: Promise<void> | null = null;

async function ensureMockWorkerStarted(): Promise<void> {
  if (!workerStartPromise) {
    workerStartPromise = import("@/mocks/browser")
      .then(async ({ worker }) => {
        await worker.start({
          onUnhandledRequest: "bypass",
          serviceWorker: {
            url: "/mockServiceWorker.js",
          },
        });
      })
      .catch((error) => {
        workerStartPromise = null;
        throw error;
      });
  }

  return workerStartPromise;
}

export function MockServiceWorkerProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [ready, setReady] = useState(() => !isMockApiEnabled());

  useEffect(() => {
    if (!isMockApiEnabled()) return;

    let active = true;

    async function startWorker() {
      try {
        await ensureMockWorkerStarted();
      } catch (error) {
        console.error("[MSW] Failed to start mock worker:", error);
      } finally {
        if (active) setReady(true);
      }
    }

    startWorker();

    return () => {
      active = false;
    };
  }, []);

  if (!ready) return null;

  return <>{children}</>;
}
