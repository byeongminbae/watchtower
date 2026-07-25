"use client";

import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Slider,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import React from "react";
import { CreateWatchRequest } from "@/lib/api/watch";
import { ApiError } from "@/lib/api/errors";
import { WatchConditionType } from "@/types/domain";

const INTERVAL_MARKS = [
  { value: 5, label: "5분" },
  { value: 60, label: "1시간" },
  { value: 720, label: "12시간" },
  { value: 1440, label: "24시간" },
];

export default function CreateWatchDialog({
  open,
  onClose,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateWatchRequest) => Promise<void>;
}) {
  const [name, setName] = React.useState("");
  const [url, setUrl] = React.useState("");
  const [conditionType, setConditionType] = React.useState<WatchConditionType>("HTML_FULL");
  const [keyword, setKeyword] = React.useState("");
  const [interval, setInterval] = React.useState(5);
  const [includeInStat, setIncludeInStat] = React.useState(true);
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const resetForm = () => {
    setName("");
    setUrl("");
    setConditionType("HTML_FULL");
    setKeyword("");
    setInterval(5);
    setIncludeInStat(true);
    setError(null);
  };

  const handleClose = () => {
    if (submitting) return;
    resetForm();
    onClose();
  };

  const canSubmit = name.trim() !== "" && url.trim() !== "" && (conditionType !== "KEYWORD" || keyword.trim() !== "");

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({
        name,
        url,
        intervalSeconds: interval * 60,
        includeInStat,
        conditions: [
          conditionType === "KEYWORD" ? { type: "KEYWORD", keyword } : { type: conditionType },
        ],
      });
      resetForm();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "와치 등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ fontWeight: 700 }}>새 와치 등록</DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}

          <TextField
            label="사이트 이름"
            placeholder="예: 회사 채용 공고"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
          />
          <TextField
            label="감시할 URL"
            placeholder="https://example.com"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            fullWidth
          />

          <TextField
            select
            label="감지 조건"
            value={conditionType}
            onChange={(e) => setConditionType(e.target.value as WatchConditionType)}
          >
            <MenuItem value="HTML_FULL">전체 변경 감지</MenuItem>
            <MenuItem value="KEYWORD">키워드 등장/소멸 감지</MenuItem>
          </TextField>
          {conditionType === "KEYWORD" && (
            <TextField label="키워드" value={keyword} onChange={(e) => setKeyword(e.target.value)} fullWidth />
          )}

          <div>
            <Typography variant="body2" gutterBottom>
              확인 주기: {interval < 60 ? `${interval}분` : `${interval / 60}시간`}
            </Typography>
            <Slider
              value={interval}
              onChange={(_, v) => setInterval(v as number)}
              min={5}
              max={1440}
              step={5}
              marks={INTERVAL_MARKS}
              color="secondary"
            />
          </div>

          <FormControlLabel
            control={
              <Switch checked={includeInStat} onChange={(e) => setIncludeInStat(e.target.checked)} color="secondary" />
            }
            label="실시간 Hot URL 통계에 포함"
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button onClick={handleClose} color="inherit" disabled={submitting}>
          취소
        </Button>
        <Button onClick={handleSubmit} variant="contained" color="secondary" disabled={!canSubmit || submitting}>
          {submitting ? <CircularProgress size={20} color="inherit" /> : "등록하기"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
