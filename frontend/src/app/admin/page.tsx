"use client";

import { Alert, Box, Card, CardContent, CircularProgress, Container, Grid, Stack, Typography } from "@mui/material";
import GroupOutlinedIcon from "@mui/icons-material/GroupOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import PaymentsOutlinedIcon from "@mui/icons-material/PaymentsOutlined";
import LoginOutlinedIcon from "@mui/icons-material/LoginOutlined";
import { LineChart } from "@mui/x-charts/LineChart";
import { adminApi } from "@/lib/api";
import { useApiData } from "@/lib/useApiData";

export default function AdminDashboardPage() {
  const {
    data,
    loading,
    error,
  } = useApiData(async () => {
    const [watchStats, userStats, paymentStats, authStats] = await Promise.all([
      adminApi.getWatchStats().then((r) => r.data),
      adminApi.getUserStats().then((r) => r.data),
      adminApi.getPaymentStats().then((r) => r.data),
      adminApi.getAuthStats().then((r) => r.data),
    ]);
    return { watchStats, userStats, paymentStats, authStats };
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 20 }}>
        <CircularProgress color="secondary" />
      </Box>
    );
  }

  if (error || !data) {
    return (
      <Container maxWidth="sm" sx={{ py: 10 }}>
        <Alert severity="error">{error ?? "통계 정보를 불러올 수 없습니다."}</Alert>
      </Container>
    );
  }

  const statCards = [
    {
      label: "총 유저 수",
      value: data.userStats.totalUsers.toLocaleString(),
      icon: <GroupOutlinedIcon />,
      color: "#3B82C4",
    },
    {
      label: "활성 와치리스트",
      value: data.watchStats.totalActiveWatches.toLocaleString(),
      icon: <VisibilityOutlinedIcon />,
      color: "#FFC94A",
    },
    {
      label: "이번 달 결제",
      value: `₩${data.paymentStats.thisMonthRevenue.toLocaleString()}`,
      icon: <PaymentsOutlinedIcon />,
      color: "#4ADE80",
    },
    {
      label: "오늘 로그인",
      value: data.authStats.todayLogins.toLocaleString(),
      icon: <LoginOutlinedIcon />,
      color: "#60A5FA",
    },
  ];

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 800 }}>
        관리자 대시보드
      </Typography>

      <Grid container spacing={2} sx={{ mb: 4 }}>
        {statCards.map((stat) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={stat.label}>
            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1, color: stat.color }}>
                  {stat.icon}
                </Stack>
                <Typography variant="h5" sx={{ fontWeight: 800 }}>
                  {stat.value}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {stat.label}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {data.watchStats.dailyTrend.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 700 }}>
              와치리스트 증가 추이
            </Typography>
            <LineChart
              height={280}
              series={[
                {
                  data: data.watchStats.dailyTrend.map((d) => d.count),
                  label: "활성 와치리스트",
                  color: "#FFC94A",
                },
              ]}
              xAxis={[{ scaleType: "point", data: data.watchStats.dailyTrend.map((d) => d.date) }]}
              sx={{
                "& .MuiChartsAxis-line": { stroke: "rgba(255,255,255,0.1)" },
                "& .MuiChartsAxis-tick": { stroke: "rgba(255,255,255,0.1)" },
              }}
            />
          </CardContent>
        </Card>
      )}
    </Container>
  );
}
