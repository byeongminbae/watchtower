import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { WatchStatus } from "@/types/domain";

const STATUS_META = {
  RUNNING: { label: "감시중", description: "정상적으로 감시하고 있어요.", variant: "secondary" },
  PAUSED: { label: "일시정지", description: "감시를 잠시 멈췄어요.", variant: "outline" },
  ILLEGAL_SUSPENDED: { label: "정책 위반 정지", description: "정책 위반으로 관리자가 감시를 정지했어요.", variant: "destructive" }
} as const;

export default function WatchStatusDot({ status, showLabel = true }: { readonly status: WatchStatus; readonly showLabel?: boolean }) {
  const meta = STATUS_META[status];
  if (!showLabel) return <span title={`${meta.label}: ${meta.description}`} aria-label={meta.label}
    className={cn("size-2 shrink-0 rounded-full", status === "RUNNING" ? "bg-primary" : status === "PAUSED" ? "bg-muted-foreground" : "bg-destructive")} />;
  return <Badge variant={meta.variant} title={meta.description}>{meta.label}</Badge>;
}
