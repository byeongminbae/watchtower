"use client";

import { createTheme } from "@mui/material/styles";

// Watchtower 테마 컨셉
// 등대의 이미지를 색으로 옮김: 하얀 탑(surface) / 파란 밤바다(background, primary)
// 노란 신호등 불빛(secondary, accent) / 밤하늘 별(divider, subtle highlight)
const watchtowerTheme = createTheme({
  palette: {
    mode: "dark",
    primary: {
      main: "#3B82C4", // 등대 불빛이 비추는 낮의 바다색 (밝은 신호 블루)
      light: "#6BA6D9",
      dark: "#1F4E79",
      contrastText: "#0B1622",
    },
    secondary: {
      main: "#FFC94A", // 등대 램프의 노란 불빛
      light: "#FFD97A",
      dark: "#D9A430",
      contrastText: "#0B1622",
    },
    background: {
      default: "#0B1622", // 깊은 밤바다
      paper: "#11202F", // 등대 탑 실내 느낌의 짙은 서피스
    },
    text: {
      primary: "#EAF2F8", // 하얀 등대 탑
      secondary: "#9FB3C8", // 안개 낀 밤하늘
    },
    divider: "rgba(255, 201, 74, 0.12)", // 별빛처럼 은은하게
    success: {
      main: "#4ADE80",
    },
    warning: {
      main: "#FFC94A",
    },
    error: {
      main: "#F87171",
    },
    info: {
      main: "#60A5FA",
    },
  },
  shape: {
    borderRadius: 12,
  },
  typography: {
    fontFamily:
      '"Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    h1: { fontWeight: 700 },
    h2: { fontWeight: 700 },
    h3: { fontWeight: 700 },
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    button: { fontWeight: 600, textTransform: "none" },
  },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: "#0B1622",
          backgroundImage: "none",
          borderBottom: "1px solid rgba(255, 201, 74, 0.12)",
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: "#11202F",
          border: "1px solid rgba(255, 255, 255, 0.06)",
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
        },
        containedSecondary: {
          color: "#0B1622",
          fontWeight: 700,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600,
        },
      },
    },
  },
});

export default watchtowerTheme;
