import type { Metadata } from "next";
import ThemeRegistry from "@/theme/ThemeRegistry";
import Header from "@/components/layout/Header";
import { AuthProvider } from "@/lib/AuthContext";

export const metadata: Metadata = {
  title: "Watchtower — 대신 지켜보는 감시탑",
  description: "매일 새로고침하시죠? 이제 변화가 생기면 먼저 알려드릴게요!",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
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
