"use client";

import * as React from "react";
import { Box, Chip, Fade, Skeleton, Stack, Typography } from "@mui/material";
import LocalFireDepartmentOutlinedIcon from "@mui/icons-material/LocalFireDepartmentOutlined";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

const ROTATION_INTERVAL_MS = 1_000;
const MAX_HOT_URLS = 10;

export default function HotUrlSection() {
  const { data: hotUrls, loading, error } = useApiData(() => watchApi.getTrendingWatches().then((r) => r.data), []);
  const rankedUrls = hotUrls?.slice(0, MAX_HOT_URLS) ?? [];
  const [activeIndex, setActiveIndex] = React.useState(0);

  React.useEffect(() => {
    if (rankedUrls.length < 2) {
      return;
    }

    const intervalId = window.setInterval(() => {
      setActiveIndex((currentIndex) => (currentIndex + 1) % rankedUrls.length);
    }, ROTATION_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [rankedUrls.length]);

  const displayIndex = rankedUrls.length > 0 ? activeIndex % rankedUrls.length : 0;
  const activeItem = rankedUrls[displayIndex];

  return (
    <Box
      aria-label="지금 가장 많이 감시하는 URL"
      sx={{
        width: "100%",
        maxWidth: 680,
        mx: "auto",
        mt: 5,
        px: { xs: 2, sm: 2.5 },
        py: 1.5,
        border: "1px solid",
        borderColor: "var(--wt-hot-border)",
        borderRadius: 2,
        bgcolor: "var(--wt-hot-surface)",
        backdropFilter: "blur(12px)",
        boxShadow:
          "inset 0 1px 0 rgba(255,255,255,0.84), 0 18px 34px -28px rgba(30,92,135,0.42)",
      }}
    >
      <Stack direction="row" alignItems="center" spacing={1.5} sx={{ minWidth: 0, minHeight: 32 }}>
        <Stack direction="row" alignItems="center" spacing={0.75} sx={{ flexShrink: 0 }}>
          <LocalFireDepartmentOutlinedIcon sx={{ color: "secondary.main", fontSize: 19 }} />
          <Typography variant="caption" sx={{ fontWeight: 700, color: "text.secondary" }}>
            지금 HOT
          </Typography>
        </Stack>

        {loading && <Skeleton variant="text" width="100%" sx={{ bgcolor: "var(--wt-hot-skeleton)" }} />}

        {!loading && (error || !activeItem) && (
          <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1, textAlign: "left" }}>
            인기 감시 URL을 집계하고 있어요
          </Typography>
        )}

        {!loading && activeItem && (
          <Fade in key={`${activeItem.url}-${displayIndex}`} timeout={240}>
            <Stack direction="row" alignItems="center" spacing={1.25} sx={{ minWidth: 0, flexGrow: 1 }}>
              <Typography
                aria-hidden="true"
                sx={{
                  color: "secondary.main",
                  fontWeight: 900,
                  fontVariantNumeric: "tabular-nums",
                  flexShrink: 0,
                }}
              >
                {displayIndex + 1}위
              </Typography>
              <Typography
                variant="body2"
                title={activeItem.url}
                sx={{
                  minWidth: 0,
                  flexGrow: 1,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                  textAlign: "left",
                  fontWeight: 600,
                }}
              >
                {activeItem.url}
              </Typography>
              <Chip
                size="small"
                label={`변화 ${activeItem.hit}회`}
                sx={{
                  display: { xs: "none", sm: "inline-flex" },
                  flexShrink: 0,
                  height: 24,
                  bgcolor: "var(--wt-hot-signal)",
                  color: "warning.main",
                }}
              />
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ flexShrink: 0, fontVariantNumeric: "tabular-nums" }}
              >
                {displayIndex + 1}/{rankedUrls.length}
              </Typography>
            </Stack>
          </Fade>
        )}
      </Stack>
    </Box>
  );
}
