import type { Metadata } from "next";
import Script from "next/script";
import ThemeRegistry from "@/theme/ThemeRegistry";
import Header from "@/components/layout/Header";
import { AuthProvider } from "@/lib/AuthContext";

export const metadata: Metadata = {
  title: "Watchtower — 대신 지켜보는 감시탑",
  description: "매일 새로고침하시죠? 이제 변화가 생기면 먼저 알려드릴게요!",
  icons: {
    icon: [{ url: "/watchtower.png", type: "image/png", sizes: "1254x1254" }],
    shortcut: ["/watchtower.png"],
    apple: [{ url: "/watchtower.png", type: "image/png", sizes: "1254x1254" }],
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
        <ThemeRegistry>
          <AuthProvider>
            <Header />
            {children}
          </AuthProvider>
        </ThemeRegistry>
      </body>
    </html>
  );
}
