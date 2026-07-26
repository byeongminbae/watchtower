"use client";

import Link from "next/link";
import { Box, Button, Chip, Container, Grid, Stack, Typography } from "@mui/material";
import NotificationsActiveOutlinedIcon from "@mui/icons-material/NotificationsActiveOutlined";
import TimelineOutlinedIcon from "@mui/icons-material/TimelineOutlined";
import TuneOutlinedIcon from "@mui/icons-material/TuneOutlined";
import LighthouseMark from "@/components/common/LighthouseMark";
import HotUrlSection from "@/components/watch/HotUrlSection";
import { useAuth } from "@/lib/AuthContext";

const FEATURES = [
  {
    icon: <TimelineOutlinedIcon fontSize="large" />,
    title: "조건 기반 감지",
    description: "HTML 전체 변경, 키워드 등장/소멸 등 원하는 조건으로 사이트 변화를 감지합니다.",
  },
  {
    icon: <NotificationsActiveOutlinedIcon fontSize="large" />,
    title: "즉시 알림",
    description: "디스코드·텔레그램으로 변경된 부분만 강조된 AI 요약을 바로 받아보세요.",
  },
  {
    icon: <TuneOutlinedIcon fontSize="large" />,
    title: "자유로운 조건 설정",
    description: "확인 주기부터 감지 조건까지, 원하는 대로 세밀하게 설정할 수 있습니다.",
  },
];

export default function HomePage() {
  const { isLoggedIn } = useAuth();

  return (
    <Box>
      {/* Hero */}
      <Box
        sx={{
          position: "relative",
          overflow: "hidden",
          background:
            "radial-gradient(ellipse 80% 60% at 50% -10%, rgba(255,201,74,0.12), transparent), linear-gradient(180deg, #0B1622 0%, #0E1B28 100%)",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
        }}
      >
        <Container maxWidth="md" sx={{ py: { xs: 10, md: 16 }, textAlign: "center" }}>
          <Box sx={{ display: "flex", justifyContent: "center", mb: 3 }}>
            <LighthouseMark size={64} />
          </Box>
          <Chip
            label="URL 변화 감지 서비스"
            size="small"
            color="secondary"
            variant="outlined"
            sx={{ mb: 2 }}
          />
          <Typography variant="h3" sx={{ fontWeight: 800, letterSpacing: -1, mb: 2 }}>
            신경 쓰지 않아도
            <br />
            놓치지 않는 변화
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 4, fontSize: 18 }}>
            매일 새로고침하시죠? 이제 변화가 생기면 먼저 알려드릴게요!
          </Typography>
          <Stack direction="row" spacing={2} justifyContent="center">
            <Button
              component={Link}
              href={isLoggedIn ? "/watches" : "/login"}
              variant="contained"
              color="secondary"
              size="large"
            >
              지금 감시 시작하기
            </Button>
            <Button
              component="a"
              href="#hot-url"
              variant="outlined"
              color="inherit"
              size="large"
            >
              지금 핫한 URL 보기
            </Button>
          </Stack>
        </Container>
      </Box>

      {/* Features */}
      <Container maxWidth="lg" sx={{ py: { xs: 8, md: 12 } }}>
        <Grid container spacing={4}>
          {FEATURES.map((feature) => (
            <Grid size={{ xs: 12, md: 4 }} key={feature.title}>
              <Stack spacing={2} alignItems="flex-start">
                <Box
                  sx={{
                    width: 56,
                    height: 56,
                    borderRadius: 2,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    backgroundColor: "rgba(255,201,74,0.1)",
                    color: "secondary.main",
                  }}
                >
                  {feature.icon}
                </Box>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  {feature.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {feature.description}
                </Typography>
              </Stack>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* Hot URL - 로그인 여부와 무관하게 항상 노출 */}
      <Box id="hot-url">
        <HotUrlSection />
      </Box>
    </Box>
  );
}
