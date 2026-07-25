"use client";

import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  Snackbar,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import LanguageIcon from "@mui/icons-material/Language";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import PauseIcon from "@mui/icons-material/Pause";
import NotificationsActiveOutlinedIcon from "@mui/icons-material/NotificationsActiveOutlined";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import AddIcon from "@mui/icons-material/Add";
import LinkIcon from "@mui/icons-material/Link";
import Link from "next/link";
import React from "react";
import { Watch, WatchCondition, WatchSnapshot } from "@/types/domain";
import WatchStatusDot from "./WatchStatusDot";
import SnapshotTimeline from "./SnapshotTimeline";
import AddConditionDialog from "./AddConditionDialog";
import { watchApi } from "@/lib/api";
import { CreateWatchConditionRequest } from "@/lib/api/watch";
import { ApiError } from "@/lib/api/errors";

const CONDITION_LABEL: Record<string, string> = {
  HTML_FULL: "전체 변경 감지",
  KEYWORD: "키워드 감지",
  REGEX: "정규식 감지",
};

export default function WatchDetailPanel({
  watch,
  snapshots,
  snapshotsLoading = false,
  onChanged,
}: {
  watch: Watch;
  snapshots: WatchSnapshot[];
  snapshotsLoading?: boolean;
  // 상태 변경(일시정지/재개, 조건 추가/삭제)이 성공했을 때 부모에게 재조회를 요청
  onChanged?: () => void;
}) {
  const isPaused = watch.status === "PAUSED";
  // watch가 바뀌면 부모에서 key={watch.id}로 이 컴포넌트를 리마운트시켜 초기값을 새로 반영함
  const [conditions, setConditions] = React.useState<WatchCondition[]>(watch.conditions ?? []);
  const [dialogOpen, setDialogOpen] = React.useState(false);
  const [togglingStatus, setTogglingStatus] = React.useState(false);
  const [sendingNotification, setSendingNotification] = React.useState(false);
  const [toast, setToast] = React.useState<{ message: string; severity: "success" | "error" } | null>(null);

  React.useEffect(() => {
    watchApi
      .getWatchConditions(watch.id)
      .then((res) => setConditions(res.data))
      .catch(() => {
        /* 조건 목록 로드 실패 시 초기값(watch.conditions) 유지 */
      });
  }, [watch.id]);

  const handleAddCondition = async (condition: CreateWatchConditionRequest) => {
    const res = await watchApi.createWatchCondition(watch.id, condition);
    setConditions((prev) => [...prev, res.data]);
  };

  const handleDeleteCondition = async (id: number) => {
    const prev = conditions;
    setConditions((c) => c.filter((cond) => cond.id !== id));
    try {
      await watchApi.deleteWatchCondition(watch.id, id);
    } catch (err) {
      setConditions(prev); // 실패 시 롤백
      setToast({
        message: err instanceof ApiError ? err.message : "조건 삭제에 실패했습니다.",
        severity: "error",
      });
    }
  };

  const handleToggleStatus = async () => {
    setTogglingStatus(true);
    try {
      await watchApi.updateWatch(watch.id, { status: isPaused ? "RUNNING" : "PAUSED" });
      onChanged?.();
    } catch (err) {
      setToast({
        message: err instanceof ApiError ? err.message : "상태 변경에 실패했습니다.",
        severity: "error",
      });
    } finally {
      setTogglingStatus(false);
    }
  };

  const handleSendTestNotification = async () => {
    setSendingNotification(true);
    try {
      await watchApi.sendTestNotification(watch.id);
      setToast({ message: "테스트 알림을 발송했습니다.", severity: "success" });
    } catch (err) {
      setToast({
        message: err instanceof ApiError ? err.message : "테스트 알림 발송에 실패했습니다.",
        severity: "error",
      });
    } finally {
      setSendingNotification(false);
    }
  };

  return (
    <Stack spacing={3}>
      {/* 헤더 */}
      <Stack direction="row" spacing={2} alignItems="flex-start">
        <Avatar
          variant="rounded"
          src={watch.faviconUrl || undefined}
          sx={{ width: 48, height: 48, bgcolor: "background.default" }}
        >
          <LanguageIcon />
        </Avatar>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              {watch.name}
            </Typography>
            <WatchStatusDot status={watch.status} />
          </Stack>
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Typography
              variant="body2"
              color="text.secondary"
              component="a"
              href={watch.url}
              target="_blank"
              sx={{ textDecoration: "none", "&:hover": { color: "primary.main" } }}
            >
              {watch.url}
            </Typography>
            <OpenInNewIcon sx={{ fontSize: 14, color: "text.secondary" }} />
          </Stack>
          {watch.lastFetchedAt && (
            <Typography variant="caption" color="text.secondary">
              최근 갱신 {new Date(watch.lastFetchedAt).toLocaleString("ko-KR")}
            </Typography>
          )}
        </Box>
        <Stack direction="row" spacing={1}>
          <Tooltip title="이 와치리스트만 보기 (링크 공유용)">
            <IconButton component={Link} href={`/watches/${watch.id}`} color="inherit">
              <LinkIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="테스트 알림 발송">
            <span>
              <IconButton color="secondary" onClick={handleSendTestNotification} disabled={sendingNotification}>
                {sendingNotification ? <CircularProgress size={20} /> : <NotificationsActiveOutlinedIcon />}
              </IconButton>
            </span>
          </Tooltip>
          <Button
            variant={isPaused ? "contained" : "outlined"}
            color={isPaused ? "secondary" : "inherit"}
            startIcon={togglingStatus ? <CircularProgress size={16} color="inherit" /> : isPaused ? <PlayArrowIcon /> : <PauseIcon />}
            onClick={handleToggleStatus}
            disabled={togglingStatus || watch.status === "PAYMENT_REQUIRED" || watch.status === "ILLEGAL_SUSPENDED"}
          >
            {isPaused ? "재개" : "일시정지"}
          </Button>
        </Stack>
      </Stack>

      {watch.status === "PAYMENT_REQUIRED" && (
        <Card sx={{ bgcolor: "rgba(255,201,74,0.08)", border: "1px solid rgba(255,201,74,0.3)" }}>
          <CardContent>
            <Typography variant="body2">
              현재 플랜 범위를 초과하여 이 와치가 정지되었습니다. 플랜을 업그레이드하면 다시 감시가
              재개됩니다.
            </Typography>
            <Button component={Link} href="/mypage/subscription" size="small" color="secondary" variant="contained" sx={{ mt: 1.5 }}>
              플랜 업그레이드
            </Button>
          </CardContent>
        </Card>
      )}

      {watch.status === "ILLEGAL_SUSPENDED" && (
        <Alert severity="error">정책 위반으로 감시가 정지되었습니다.</Alert>
      )}

      <Divider />

      {/* 조건 목록 */}
      <Box>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            감지 조건
          </Typography>
          <Button
            size="small"
            startIcon={<AddIcon />}
            disabled={conditions.length >= 3}
            onClick={() => setDialogOpen(true)}
          >
            조건 추가
          </Button>
        </Stack>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {conditions.length === 0 && (
            <Typography variant="caption" color="text.secondary">
              등록된 조건이 없습니다. 조건을 추가해주세요.
            </Typography>
          )}
          {conditions.map((c) => (
            <Chip
              key={c.id}
              label={
                c.type === "KEYWORD"
                  ? `키워드: ${c.keyword}`
                  : c.type === "REGEX"
                    ? `정규식: ${c.regex}`
                    : CONDITION_LABEL[c.type]
              }
              variant="outlined"
              onDelete={() => handleDeleteCondition(c.id)}
            />
          ))}
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: "block" }}>
          조건은 최대 3개까지 등록할 수 있습니다. (확인 주기 {watch.intervalSeconds / 60}분)
        </Typography>
      </Box>

      <Divider />

      {snapshotsLoading ? (
        <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
          <CircularProgress color="secondary" size={24} />
        </Box>
      ) : (
        <SnapshotTimeline snapshots={snapshots} />
      )}

      <AddConditionDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onAdd={handleAddCondition}
        currentCount={conditions.length}
      />

      <Snackbar
        open={toast !== null}
        autoHideDuration={4000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        {toast ? (
          <Alert severity={toast.severity} onClose={() => setToast(null)} sx={{ width: "100%" }}>
            {toast.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </Stack>
  );
}
