"use client";

import RequireAuth from "@/components/common/RequireAuth";

export default function MyPageLayout({ children }: { children: React.ReactNode }) {
  return <RequireAuth>{children}</RequireAuth>;
}
