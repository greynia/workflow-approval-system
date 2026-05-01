import "./globals.css";
import { RootProviders } from "@/components/providers/root-providers";
import { Geist, Noto_Sans_TC } from "next/font/google";
import { cn } from "@/lib/utils";

const geist = Geist({ subsets: ['latin'], variable: '--font-geist' });
const notoSansTC = Noto_Sans_TC({
  subsets: ['latin'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-noto',
});

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
    <html lang="zh-TW" className={cn("h-full antialiased font-sans", geist.variable, notoSansTC.variable)}>
      <body className="min-h-full flex flex-col">
        <RootProviders>{children}</RootProviders>
      </body>
    </html>
  );
}
