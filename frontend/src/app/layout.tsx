import type { Metadata } from "next";
import Script from "next/script";
import Header from "@/components/layout/Header";
import AppShell from "@/components/layout/AppShell";
import { TooltipProvider } from "@/components/ui/tooltip";
import "./globals.css";
import { AuthProvider } from "@/lib/AuthContext";

export const metadata: Metadata = {
  title: "Watchtower — 대신 지켜보는 감시탑",
  description: "매일 새로고침하시죠? 이제 변화가 생기면 먼저 알려드릴게요!",
  icons: {
    icon: [{ url: "/watchtower-icon.svg", type: "image/svg+xml", sizes: "any" }],
    shortcut: ["/watchtower-icon.svg"],
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        {process.env.NODE_ENV === "development" && (
          <>
            <Script
              src="//unpkg.com/react-grab/dist/index.global.js"
              crossOrigin="anonymous"
              strategy="beforeInteractive"
            />
            <Script
              src="//unpkg.com/react-scan/dist/auto.global.js"
              crossOrigin="anonymous"
              strategy="beforeInteractive"
            />
          </>
        )}
      </head>
      <body>
        <AuthProvider>
          <TooltipProvider delay={300}>
          <a className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground" href="#main-content">본문으로 이동</a>
          <Header />
          <AppShell>{children}</AppShell>
          </TooltipProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
