"use client";

import Link from "next/link";
import { Box, Button, Chip, Container, Stack, Typography } from "@mui/material";
import ArrowForwardRoundedIcon from "@mui/icons-material/ArrowForwardRounded";
import LighthouseMark from "@/components/common/LighthouseMark";
import LandingStory from "@/components/home/LandingStory";
import HotUrlSection from "@/components/watch/HotUrlSection";
import { useAuth } from "@/lib/AuthContext";

export default function HomePage() {
  const { isLoggedIn } = useAuth();

  return (
    <Box component="main">
      <Box
        component="section"
        sx={{
          position: "relative",
          overflow: "hidden",
          background:
            "radial-gradient(ellipse 72% 58% at 50% -8%, rgba(255,201,74,0.16), transparent), linear-gradient(180deg, #0B1622 0%, #0E1B28 100%)",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
          "&::after": {
            content: '""',
            position: "absolute",
            inset: 0,
            pointerEvents: "none",
            opacity: 0.32,
            backgroundImage:
              "linear-gradient(rgba(255,255,255,0.018) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.018) 1px, transparent 1px)",
            backgroundSize: "48px 48px",
            maskImage: "linear-gradient(to bottom, black, transparent 75%)",
          },
        }}
      >
        <Container
          maxWidth="md"
          sx={{ position: "relative", zIndex: 1, py: { xs: 10, md: 15 }, textAlign: "center" }}
        >
          <Box sx={{ display: "flex", justifyContent: "center", mb: 3 }}>
            <LighthouseMark size={64} />
          </Box>
          <Chip
            label="놓치고 싶지 않은 페이지를 위한 변화 감지"
            size="small"
            color="secondary"
            variant="outlined"
            sx={{ mb: 2.5 }}
          />
          <Typography
            variant="h2"
            sx={{
              mb: 2.5,
              fontWeight: 850,
              fontSize: { xs: "1.8rem", sm: "3.35rem", md: "4rem" },
              lineHeight: 1.12,
              letterSpacing: { xs: -1.3, md: -2 },
              textWrap: "balance",
            }}
          >
            기다리던 소식,
            <br />
            이번에는 놓치지 마세요
          </Typography>
          <Typography
            color="text.secondary"
            sx={{
              maxWidth: 600,
              mx: "auto",
              mb: 4.5,
              fontSize: { xs: 16, sm: 18 },
              lineHeight: 1.7,
              textWrap: "pretty",
            }}
          >
            매일 페이지를 열어보는 대신, 변화가 생긴 순간만 확인하세요.
            <br />
            Watchtower가 대신 지켜보고 달라진 내용을 AI로 요약해 드립니다.
          </Typography>
          <Button
            component={Link}
            href={isLoggedIn ? "/watches" : "/login"}
            variant="contained"
            color="secondary"
            size="large"
            endIcon={<ArrowForwardRoundedIcon />}
            sx={{ px: 3.5, py: 1.35, fontWeight: 750 }}
          >
            지금 감시 시작하기
          </Button>
          <HotUrlSection />
        </Container>
      </Box>

      <LandingStory />

      <Box
        component="section"
        sx={{
          bgcolor: "background.default",
          borderTop: "1px solid",
          borderColor: "divider",
        }}
      >
        <Container maxWidth="md" sx={{ py: { xs: 10, md: 14 }, textAlign: "center" }}>
          <Typography
            variant="h4"
            sx={{
              mb: 2,
              fontWeight: 850,
              fontSize: { xs: "1.8rem", sm: "2.125rem" },
              letterSpacing: -1,
              textWrap: "balance",
            }}
          >
            다음 중요한 변화는
            <br />
            놓치지 않도록
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 4, lineHeight: 1.75 }}>
            지금 가장 기다리는 페이지부터 등록해 보세요.
          </Typography>
          <Stack direction="row" justifyContent="center">
            <Button
              component={Link}
              href={isLoggedIn ? "/watches" : "/login"}
              variant="contained"
              color="secondary"
              size="large"
              endIcon={<ArrowForwardRoundedIcon />}
            >
              무료로 감시 시작하기
            </Button>
          </Stack>
        </Container>
      </Box>
    </Box>
  );
}
