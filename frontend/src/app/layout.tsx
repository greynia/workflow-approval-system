import "./globals.css";
import { RootProviders } from "@/components/providers/root-providers";

export const metadata = {
  title: "Workflow Approval System",
  description: "Internal workflow approval management system",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-TW" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <RootProviders>{children}</RootProviders>
      </body>
    </html>
  );
}
