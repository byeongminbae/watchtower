import { Box, Container, Grid, Stack, Typography } from "@mui/material";
import CampaignOutlinedIcon from "@mui/icons-material/CampaignOutlined";
import ConfirmationNumberOutlinedIcon from "@mui/icons-material/ConfirmationNumberOutlined";
import SchoolOutlinedIcon from "@mui/icons-material/SchoolOutlined";
import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import NotificationsNoneOutlinedIcon from "@mui/icons-material/NotificationsNoneOutlined";
import TravelExploreOutlinedIcon from "@mui/icons-material/TravelExploreOutlined";

const COMMON_EXPERIENCES = [
  {
    icon: <CampaignOutlinedIcon />,
    title: "신청 기간에 알림을 보내주지 않는 정부 지원사업",
  },
  {
    icon: <SchoolOutlinedIcon />,
    title: "새 공지를 확인하기 위해 반복하는 학사공지 새로고침",
  },
  {
    icon: <ConfirmationNumberOutlinedIcon />,
    title: "‘예매 시작’을 기다리며 반복하는 공연·행사 페이지 새로고침",
  },
] as const;

const HOW_IT_WORKS = [
  {
    icon: <TravelExploreOutlinedIcon />,
    number: "01",
    title: "기다리는 페이지를 등록하세요",
    description: "지원사업, 학사공지, 공연 예매처럼 놓치고 싶지 않은 URL 하나면 충분합니다.",
  },
  {
    icon: <AutoAwesomeOutlinedIcon />,
    number: "02",
    title: "알고 싶은 변화를 말해 주세요",
    description: "새 공지가 올라올 때, 특정 문구가 등장할 때처럼 필요한 변화만 골라 감지합니다.",
  },
  {
    icon: <NotificationsNoneOutlinedIcon />,
    number: "03",
    title: "변화가 생긴 순간만 확인하세요",
    description: "달라진 내용을 AI가 요약합니다. 이제 매일 페이지를 열 필요가 없습니다.",
  },
] as const;

export default function LandingStory() {
  return (
    <>
      <Box
        component="section"
        sx={{
          bgcolor: "background.default",
          "& .MuiTypography-root": { wordBreak: "keep-all", overflowWrap: "break-word" },
        }}
      >
        <Container maxWidth="lg" sx={{ py: { xs: 9, md: 14 } }}>
          <Grid container spacing={{ xs: 4, md: 12 }}>
            <Grid size={{ xs: 12, md: 5 }} sx={{ order: { xs: 0, md: 2 } }}>
              <Typography
                variant="h4"
                sx={{
                  fontWeight: 850,
                  fontSize: { xs: "1.7rem", sm: "2.125rem" },
                  lineHeight: 1.24,
                  letterSpacing: -1,
                  textWrap: "balance",
                }}
              >
                이런 경험,
                <br />
                한 번쯤 있지 않으셨나요?
              </Typography>
            </Grid>
            <Grid size={{ xs: 12, md: 7 }} sx={{ order: { xs: 1, md: 1 } }}>
              <Stack spacing={0}>
                {COMMON_EXPERIENCES.map((experience) => (
                  <Stack
                    direction="row"
                    spacing={{ xs: 2, sm: 2.5 }}
                    alignItems="flex-start"
                    key={experience.title}
                    sx={{
                      py: { xs: 2.75, sm: 3.5 },
                      borderBottom: "1px solid",
                      borderColor: "divider",
                      "&:first-of-type": { pt: { xs: 0, md: 0.5 } },
                    }}
                  >
                    <Box sx={{ color: "secondary.main", display: "flex", pt: 0.25, flexShrink: 0 }}>
                      {experience.icon}
                    </Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, lineHeight: 1.45, textWrap: "pretty" }}>
                      {experience.title.includes("공연·행사") ? (
                        <>
                          {experience.title.slice(0, experience.title.indexOf("공연·행사"))}
                          <Box component="span" sx={{ whiteSpace: "nowrap" }}>
                            공연·행사
                          </Box>
                          {experience.title.slice(experience.title.indexOf("공연·행사") + "공연·행사".length)}
                        </>
                      ) : (
                        experience.title
                      )}
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            </Grid>
          </Grid>
        </Container>
      </Box>

      <Box
        component="section"
        sx={{
          position: "relative",
          overflow: "hidden",
          bgcolor: "background.paper",
          backgroundImage: "var(--wt-story-atmosphere)",
          borderTop: "1px solid",
          borderColor: "divider",
          "& .MuiTypography-root": { wordBreak: "keep-all", overflowWrap: "break-word" },
        }}
      >
        <Container maxWidth="lg" sx={{ py: { xs: 11, md: 17 } }}>
          <Grid container spacing={{ xs: 7, md: 12 }}>
            <Grid size={{ xs: 12, md: 5 }}>
              <Typography color="secondary.main" sx={{ mb: 2, fontWeight: 700 }}>
                새로고침은 Watchtower에게
              </Typography>
              <Typography
                variant="h4"
                sx={{
                  mb: 3,
                  fontWeight: 850,
                  fontSize: { xs: "1.7rem", sm: "2.125rem" },
                  lineHeight: 1.24,
                  letterSpacing: -1,
                  textWrap: "balance",
                }}
              >
                변화는 대신 지켜보고,
                <br />
                필요한 내용만 알려드릴게요
              </Typography>
              <Typography color="text.secondary" sx={{ maxWidth: 460, lineHeight: 1.8, textWrap: "pretty" }}>
                계속 확인하지 않아도 괜찮습니다.{" "}
                <Box component="span" sx={{ whiteSpace: "nowrap" }}>
                  Watchtower는 등록한
                </Box>{" "}
                페이지의 변화를 감지하고, 무엇이 달라졌는지 AI로 짧고 이해하기 쉽게 요약합니다.
              </Typography>
            </Grid>
            <Grid size={{ xs: 12, md: 7 }}>
              <Stack spacing={0}>
                {HOW_IT_WORKS.map((step) => (
                  <Stack
                    direction="row"
                    spacing={{ xs: 2, sm: 3 }}
                    key={step.number}
                    sx={{ py: 3.5, borderBottom: "1px solid", borderColor: "divider" }}
                  >
                    <Typography
                      aria-hidden="true"
                      sx={{
                        color: "secondary.main",
                        fontWeight: 800,
                        fontVariantNumeric: "tabular-nums",
                        pt: 0.4,
                      }}
                    >
                      {step.number}
                    </Typography>
                    <Box sx={{ flexGrow: 1 }}>
                      <Stack direction="row" spacing={1.25} alignItems="center" sx={{ mb: 1 }}>
                        <Box sx={{ display: "flex", color: "secondary.main" }}>{step.icon}</Box>
                        <Typography variant="h6" sx={{ fontWeight: 750 }}>
                          {step.title}
                        </Typography>
                      </Stack>
                      <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.75, textWrap: "pretty" }}>
                        {step.description}
                      </Typography>
                    </Box>
                  </Stack>
                ))}
              </Stack>
            </Grid>
          </Grid>
        </Container>
      </Box>
    </>
  );
}
