import { requireServerSession } from "@/lib/server/session";
import RequireAdmin from "@/components/common/RequireAdmin";

export default async function AdminLayout({ children }: { readonly children: React.ReactNode }) {
  await requireServerSession(true);
  return <RequireAdmin>{children}</RequireAdmin>;
}
