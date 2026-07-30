import { Box, Chip, Tooltip } from "@mui/material";
import type { WatchStatus } from "@/types/domain";

const STATUS_META: Record<WatchStatus, { label: string; color: string; description: string }> = {
  RUNNING: {
    label: "감시중",
    color: "var(--wt-status-success)",
    description: "정상적으로 감시가 진행 중입니다.",
  },
  PAUSED: {
    label: "일시정지",
    color: "var(--wt-text-tertiary)",
    description: "사용자가 감시를 일시정지했습니다.",
  },
  PAYMENT_REQUIRED: {
    label: "결제 필요",
    color: "var(--wt-status-warning)",
    description: "플랜 범위를 초과하여 결제가 필요합니다.",
  },
  ILLEGAL_SUSPENDED: {
    label: "정책 위반 정지",
    color: "var(--wt-status-error)",
    description: "정책 위반으로 관리자에 의해 정지되었습니다.",
  },
};

export default function WatchStatusDot({
  status,
  showLabel = true,
}: {
  status: WatchStatus;
  showLabel?: boolean;
}) {
  const meta = STATUS_META[status];

  if (!showLabel) {
    return (
      <Tooltip title={`${meta.label} · ${meta.description}`} arrow>
        <Box
          sx={{
            width: 10,
            height: 10,
            borderRadius: "50%",
            backgroundColor: meta.color,
            boxShadow: status === "RUNNING" ? `0 0 6px ${meta.color}` : "none",
            flexShrink: 0,
          }}
        />
      </Tooltip>
    );
  }

  return (
    <Chip
      size="small"
      label={meta.label}
      sx={{
        backgroundColor: `color-mix(in srgb, ${meta.color} 13%, transparent)`,
        color: meta.color,
        border: `1px solid color-mix(in srgb, ${meta.color} 34%, transparent)`,
        fontWeight: 600,
      }}
    />
  );
}
