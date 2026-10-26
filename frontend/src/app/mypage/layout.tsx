import RequireAuth from "@/components/common/RequireAuth";
import { requireServerSession } from "@/lib/server/session";

export default async function MyPageLayout({ children }: { readonly children: React.ReactNode }) {
  await requireServerSession();
  return <RequireAuth>{children}</RequireAuth>;
}
