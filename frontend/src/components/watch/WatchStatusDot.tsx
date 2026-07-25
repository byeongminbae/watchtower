import { Box, Chip, Tooltip } from "@mui/material";
import { WatchStatus } from "@/types/domain";

const STATUS_META: Record<WatchStatus, { label: string; color: string; description: string }> = {
  RUNNING: { label: "감시중", color: "#4ADE80", description: "정상적으로 감시가 진행 중입니다." },
  PAUSED: { label: "일시정지", color: "#9FB3C8", description: "사용자가 감시를 일시정지했습니다." },
  PAYMENT_REQUIRED: {
    label: "결제 필요",
    color: "#FFC94A",
    description: "플랜 범위를 초과하여 결제가 필요합니다.",
  },
  ILLEGAL_SUSPENDED: {
    label: "정책 위반 정지",
    color: "#F87171",
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
        backgroundColor: `${meta.color}22`,
        color: meta.color,
        border: `1px solid ${meta.color}55`,
        fontWeight: 600,
      }}
    />
  );
}
