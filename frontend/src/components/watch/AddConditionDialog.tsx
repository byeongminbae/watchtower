"use client";

import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from "@mui/material";
import React from "react";
import { WatchConditionType } from "@/types/domain";
import { CreateWatchConditionRequest } from "@/lib/api/watch";
import { ApiError } from "@/lib/api/errors";

export default function AddConditionDialog({
  open,
  onClose,
  onAdd,
  currentCount,
}: {
  open: boolean;
  onClose: () => void;
  onAdd: (condition: CreateWatchConditionRequest) => Promise<void>;
  currentCount: number;
}) {
  const [type, setType] = React.useState<WatchConditionType>("HTML_FULL");
  const [keyword, setKeyword] = React.useState("");
  const [regex, setRegex] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const reachedLimit = currentCount >= 3;

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await onAdd({
        type,
        keyword: type === "KEYWORD" ? keyword : undefined,
        regex: type === "REGEX" ? regex : undefined,
      });
      setKeyword("");
      setRegex("");
      setType("HTML_FULL");
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "조건 추가에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const canSubmit =
    !reachedLimit && (type === "HTML_FULL" || (type === "KEYWORD" && keyword.trim() !== "") || (type === "REGEX" && regex.trim() !== ""));

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle sx={{ fontWeight: 700 }}>감지 조건 추가</DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {reachedLimit && <Alert severity="warning">조건은 최대 3개까지 등록할 수 있습니다.</Alert>}

          <TextField
            select
            label="감지 조건"
            value={type}
            onChange={(e) => setType(e.target.value as WatchConditionType)}
            disabled={reachedLimit}
          >
            <MenuItem value="HTML_FULL">전체 변경 감지</MenuItem>
            <MenuItem value="KEYWORD">키워드 등장/소멸 감지</MenuItem>
            <MenuItem value="REGEX">정규식 감지</MenuItem>
          </TextField>

          {type === "KEYWORD" && (
            <TextField
              label="키워드"
              placeholder="예: 재입고"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              disabled={reachedLimit}
              autoFocus
            />
          )}
          {type === "REGEX" && (
            <TextField
              label="정규식"
              placeholder="예: 당첨자\s?발표"
              value={regex}
              onChange={(e) => setRegex(e.target.value)}
              disabled={reachedLimit}
              autoFocus
            />
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button onClick={onClose} color="inherit" disabled={submitting}>
          취소
        </Button>
        <Button onClick={handleSubmit} variant="contained" color="secondary" disabled={!canSubmit || submitting}>
          {submitting ? <CircularProgress size={20} color="inherit" /> : "추가하기"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
