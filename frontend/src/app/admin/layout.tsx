"use client";

import RequireAdmin from "@/components/common/RequireAdmin";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <RequireAdmin>{children}</RequireAdmin>;
}
