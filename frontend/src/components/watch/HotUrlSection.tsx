"use client";

import { Box, Card, CardContent, Chip, CircularProgress, Container, Stack, Typography } from "@mui/material";
import LocalFireDepartmentIcon from "@mui/icons-material/LocalFireDepartment";
import { watchApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

export default function HotUrlSection() {
  const { data: hotUrls, loading, error } = useApiData(() => watchApi.getTrendingWatches().then((r) => r.data), []);

  return (
    <Box sx={{ bgcolor: "background.paper", borderTop: "1px solid rgba(255,255,255,0.06)" }}>
      <Container maxWidth="sm" sx={{ py: { xs: 6, md: 10 } }}>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
          <LocalFireDepartmentIcon color="secondary" />
          <Typography variant="h5" sx={{ fontWeight: 800 }}>
            지금 가장 핫한 URL
          </Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
          오늘 하루 동안 가장 활발하게 변화가 감지된 URL이에요.
        </Typography>

        {loading && (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress color="secondary" size={24} />
          </Box>
        )}

        {!loading && error && (
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 4 }}>
            현재는 데이터를 불러올 수 없습니다
          </Typography>
        )}

        {!loading && !error && hotUrls?.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 4 }}>
            아직 집계된 데이터가 없어요.
          </Typography>
        )}

        <Stack spacing={1.5}>
          {hotUrls?.map((item, idx) => (
            <Card key={item.url} variant="outlined">
              <CardContent
                sx={{ display: "flex", alignItems: "center", gap: 2, "&:last-child": { pb: 2 } }}
              >
                <Typography
                  variant="h6"
                  sx={{
                    width: 32,
                    fontWeight: 800,
                    color: idx < 3 ? "secondary.main" : "text.secondary",
                  }}
                >
                  {idx + 1}
                </Typography>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {item.url}
                  </Typography>
                </Box>
                <Chip
                  size="small"
                  label={`변화 ${item.hit}회`}
                  sx={{ bgcolor: "rgba(255,201,74,0.12)", color: "secondary.main" }}
                />
              </CardContent>
            </Card>
          ))}
        </Stack>
      </Container>
    </Box>
  );
}
