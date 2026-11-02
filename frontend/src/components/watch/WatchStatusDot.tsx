import { CircleAlert, LoaderCircle, Pause, Radar, ShieldBan, type LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { WatchStatus } from "@/types/domain";

type StatusMeta = {
  readonly label: string;
  readonly description: string;
  readonly variant: "secondary" | "outline" | "destructive";
  readonly dot: string;
  readonly icon: LucideIcon;
  readonly spinning: boolean;
};

export const WATCH_STATUS_META: Record<WatchStatus, StatusMeta> = {
  CRAWLING_READY: { label: "감시중", description: "다음 확인을 기다리고 있어요.", variant: "secondary", dot: "bg-primary", icon: Radar, spinning: false },
  CRAWLING: { label: "확인 중", description: "지금 페이지를 확인하고 있어요.", variant: "secondary", dot: "bg-primary animate-pulse", icon: LoaderCircle, spinning: true },
  CRAWLING_ERROR: { label: "확인 실패", description: "페이지를 확인하지 못했어요. 다시 시도해 주세요.", variant: "destructive", dot: "bg-destructive", icon: CircleAlert, spinning: false },
  USER_PAUSED: { label: "일시정지", description: "감시를 잠시 멈췄어요.", variant: "outline", dot: "bg-muted-foreground", icon: Pause, spinning: false },
  ADMIN_PAUSED: { label: "관리자 정지", description: "정책 위반 등으로 관리자가 감시를 정지했어요.", variant: "destructive", dot: "bg-destructive", icon: ShieldBan, spinning: false }
};

export default function WatchStatusDot({ status, showLabel = true }: { readonly status: WatchStatus; readonly showLabel?: boolean }) {
  const meta = WATCH_STATUS_META[status];
  if (!showLabel) return <span title={`${meta.label}: ${meta.description}`} aria-label={meta.label} className={cn("size-2 shrink-0 rounded-full", meta.dot)} />;
  const Icon = meta.icon;
  return <Badge variant={meta.variant} title={meta.description}>
    <Icon data-icon="inline-start" aria-hidden="true" className={cn(meta.spinning && "animate-spin")} />{meta.label}
  </Badge>;
}
