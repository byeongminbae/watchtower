"use client";

import {
  Box,
  Card,
  CardContent,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import CompareIcon from "@mui/icons-material/Compare";
import ArticleOutlinedIcon from "@mui/icons-material/ArticleOutlined";
import React from "react";
import { WatchSnapshot } from "@/types/domain";

export default function SnapshotTimeline({ snapshots }: { snapshots: WatchSnapshot[] }) {
  const [viewMode, setViewMode] = React.useState<"full" | "diff">("diff");

  return (
    <Stack spacing={2}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="subtitle1" sx={{fontWeight: 700}}>
          변경 타임라인
        </Typography>
        <ToggleButtonGroup
          size="small"
          value={viewMode}
          exclusive
          onChange={(_, v) => v && setViewMode(v)}
        >
          <ToggleButton value="diff">
            <CompareIcon fontSize="small" sx={{ mr: 0.5 }} />
            변경 부분만
          </ToggleButton>
          <ToggleButton value="full">
            <ArticleOutlinedIcon fontSize="small" sx={{ mr: 0.5 }} />
            전체 페이지
          </ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      <Typography variant="caption" color="text.secondary">
        스냅샷은 최대 1주일간 보관 및 조회가 가능합니다.
      </Typography>

      <Stack spacing={0} sx={{ position: "relative" }}>
        {snapshots.map((snapshot, idx) => (
          <Box key={snapshot.id} sx={{ display: "flex", gap: 2 }}>
            <Stack alignItems="center" sx={{ pt: 1 }}>
              <Box
                sx={{
                  width: 10,
                  height: 10,
                  borderRadius: "50%",
                  bgcolor: "secondary.main",
                  boxShadow: "0 0 6px rgba(255,201,74,0.6)",
                }}
              />
              {idx !== snapshots.length - 1 && (
                <Box sx={{ width: "1px", flexGrow: 1, bgcolor: "divider", minHeight: 60 }} />
              )}
            </Stack>
            <Card variant="outlined" sx={{ flexGrow: 1, mb: 2 }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(snapshot.notifiedAt).toLocaleString("ko-KR")}
                  </Typography>
                </Stack>
                <Typography variant="body2">{snapshot.aiDiffSummary}</Typography>
                <Box
                  sx={{
                    mt: 1.5,
                    height: 160,
                    borderRadius: 1.5,
                    bgcolor: "background.default",
                    border: "1px dashed",
                    borderColor: "divider",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Typography variant="caption" color="text.secondary">
                    {viewMode === "diff" ? "변경 강조 스크린샷" : "전체 페이지 스크린샷"}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Box>
        ))}
      </Stack>
    </Stack>
  );
}
