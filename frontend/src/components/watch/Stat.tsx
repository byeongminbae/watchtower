import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

export default function Stat({ icon: Icon, label, children }: { readonly icon: LucideIcon; readonly label: string; readonly children: ReactNode }) {
  return <div className="flex min-w-0 flex-col gap-1">
    <dt className="flex items-center gap-1.5 text-xs text-muted-foreground"><Icon aria-hidden="true" className="size-3.5" />{label}</dt>
    <dd className="truncate text-base font-medium">{children}</dd>
  </div>;
}
