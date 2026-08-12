"use client";

import { createTheme } from "@mui/material/styles";

const watchtowerTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#246B9E",
      light: "#5A9DC9",
      dark: "#18547F",
      contrastText: "#FFFFFF",
    },
    secondary: {
      main: "#F4B83A",
      light: "#FFD66F",
      dark: "#E6A622",
      contrastText: "#102A43",
    },
    background: {
      default: "#F6FAFE",
      paper: "#FFFFFF",
    },
    text: {
      primary: "#102A43",
      secondary: "#526D82",
      disabled: "#7D94A6",
    },
    divider: "#D7E5EF",
    success: {
      main: "#16845B",
    },
    warning: {
      main: "#B86E10",
    },
    error: {
      main: "#C54444",
    },
    info: {
      main: "#2477B3",
    },
  },
  shape: {
    borderRadius: 14,
  },
  typography: {
    fontFamily:
      '"Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    h1: { fontWeight: 720, letterSpacing: "-0.025em", textWrap: "balance" },
    h2: { fontWeight: 720, letterSpacing: "-0.025em", textWrap: "balance" },
    h3: { fontWeight: 700, letterSpacing: "-0.02em", textWrap: "balance" },
    h4: { fontWeight: 700, letterSpacing: "-0.02em", textWrap: "balance" },
    h5: { fontWeight: 680, letterSpacing: "-0.015em", textWrap: "balance" },
    h6: { fontWeight: 660, letterSpacing: "-0.01em", textWrap: "balance" },
    body1: { lineHeight: 1.65 },
    body2: { lineHeight: 1.55 },
    button: { fontWeight: 650, textTransform: "none" },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        ":root": {
          "--wt-surface-canvas": "#F6FAFE",
          "--wt-surface-primary": "#FFFFFF",
          "--wt-surface-soft": "#EDF6FC",
          "--wt-surface-tint": "#E2F1FB",
          "--wt-text-primary": "#102A43",
          "--wt-text-secondary": "#526D82",
          "--wt-text-tertiary": "#7D94A6",
          "--wt-border-default": "#D7E5EF",
          "--wt-accent-primary": "#246B9E",
          "--wt-accent-hover": "#18547F",
          "--wt-signal-primary": "#F4B83A",
          "--wt-signal-hover": "#E6A622",
          "--wt-signal-soft": "#FFF2C7",
          "--wt-status-success": "#16845B",
          "--wt-status-warning": "#B86E10",
          "--wt-status-error": "#C54444",
          "--wt-status-info": "#2477B3",
          "--wt-body-atmosphere": "radial-gradient(circle at 12% 8%, rgba(255, 242, 199, 0.82), transparent 30rem), radial-gradient(circle at 88% 18%, rgba(197, 228, 247, 0.72), transparent 34rem), linear-gradient(180deg, #FBFDFF 0%, #F6FAFE 42%, #EEF7FC 100%)",
          "--wt-hero-atmosphere": "radial-gradient(ellipse 56% 60% at 20% 0%, rgba(255,236,174,0.9), transparent), radial-gradient(ellipse 60% 58% at 86% 18%, rgba(183,224,247,0.76), transparent), linear-gradient(180deg, rgba(255,255,255,0.96) 0%, #EEF7FC 100%)",
          "--wt-login-atmosphere": "radial-gradient(ellipse 58% 56% at 30% 0%, rgba(255,236,174,0.78), transparent), radial-gradient(ellipse 48% 52% at 88% 18%, rgba(183,224,247,0.64), transparent)",
          "--wt-story-atmosphere": "radial-gradient(circle at 80% 20%, rgba(255,226,143,0.28), transparent 34%), linear-gradient(145deg, rgba(255,255,255,0.92), rgba(237,246,252,0.78))",
          "--wt-grid-pattern": "linear-gradient(rgba(36,107,158,0.045) 1px, transparent 1px), linear-gradient(90deg, rgba(36,107,158,0.045) 1px, transparent 1px)",
          "--wt-hot-surface": "rgba(255,255,255,0.62)",
          "--wt-hot-border": "rgba(244,184,58,0.38)",
          "--wt-hot-signal": "rgba(244,184,58,0.16)",
          "--wt-hot-skeleton": "rgba(36,107,158,0.1)",
          "--wt-mark-sea": "#246B9E",
          "--wt-mark-sky": "#5A9DC9",
          "--wt-mark-light": "#FFD66F",
          "--wt-mark-tower": "#EAF2F8",
          "--wt-mark-stripe": "#18547F",
          "--wt-mark-signal": "#F4B83A",
          "--wt-mark-wave": "#BDE2F5",
          "--wt-motion-press": "120ms",
          "--wt-motion-micro": "150ms",
          "--wt-motion-surface": "220ms",
        },
        html: {
          scrollBehavior: "smooth",
          backgroundColor: "var(--wt-surface-canvas)",
        },
        body: {
          minHeight: "100dvh",
          backgroundColor: "var(--wt-surface-canvas)",
          backgroundImage: "var(--wt-body-atmosphere)",
          backgroundAttachment: "fixed",
        },
        "::selection": {
          backgroundColor: "var(--wt-signal-soft)",
          color: "var(--wt-text-primary)",
        },
        "*": {
          scrollbarColor: "#A8C9DE transparent",
        },
        "@media (prefers-reduced-motion: reduce)": {
          html: { scrollBehavior: "auto" },
          "*, *::before, *::after": {
            animationDuration: "0.01ms !important",
            animationIterationCount: "1 !important",
            transitionDuration: "0.01ms !important",
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          color: "#102A43",
          backgroundColor: "rgba(255, 255, 255, 0.78)",
          backgroundImage:
            "linear-gradient(110deg, rgba(255,255,255,0.92), rgba(246,250,254,0.72))",
          backdropFilter: "blur(18px) saturate(140%)",
          borderBottom: "1px solid rgba(215, 229, 239, 0.86)",
          boxShadow: "0 10px 34px -28px rgba(30, 92, 135, 0.55)",
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage:
            "linear-gradient(145deg, rgba(255,255,255,0.96), rgba(248,252,255,0.9))",
          borderColor: "#D7E5EF",
          boxShadow:
            "inset 0 1px 0 rgba(255,255,255,0.82), 0 18px 45px -30px rgba(30,92,135,0.32), 0 8px 22px -18px rgba(16,42,67,0.18)",
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: "rgba(255,255,255,0.9)",
          backgroundImage:
            "linear-gradient(145deg, rgba(255,255,255,0.98), rgba(241,248,253,0.88))",
          border: "1px solid rgba(215,229,239,0.92)",
          boxShadow:
            "inset 0 1px 0 rgba(255,255,255,0.9), 0 18px 45px -30px rgba(30,92,135,0.32), 0 8px 22px -18px rgba(16,42,67,0.18)",
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition:
            "transform var(--wt-motion-press) ease-out, box-shadow var(--wt-motion-surface) cubic-bezier(0.16, 1, 0.3, 1), background-color var(--wt-motion-micro) ease-out, border-color var(--wt-motion-micro) ease-out",
          "&:active": {
            transform: "scale(0.975)",
          },
          "&:focus-visible": {
            outline: "2px solid #246B9E",
            outlineOffset: 3,
          },
          "@media (prefers-reduced-motion: reduce)": {
            "&:active": { transform: "none" },
          },
        },
        containedSecondary: {
          color: "#102A43",
          fontWeight: 700,
          backgroundImage: "linear-gradient(135deg, #FFD66F 0%, #F4B83A 72%)",
          boxShadow: "0 14px 30px -18px rgba(184,110,16,0.62), inset 0 1px 0 rgba(255,255,255,0.65)",
          "&:hover": {
            backgroundImage: "linear-gradient(135deg, #FFDA7D 0%, #E6A622 78%)",
            boxShadow:
              "0 18px 34px -18px rgba(184,110,16,0.68), inset 0 1px 0 rgba(255,255,255,0.72)",
          },
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: "rgba(255,255,255,0.76)",
          transition:
            "background-color var(--wt-motion-micro) ease-out, box-shadow var(--wt-motion-micro) ease-out",
          "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: "#8BB7D3" },
          "&.Mui-focused": { boxShadow: "0 0 0 3px rgba(36,107,158,0.12)" },
          "&.Mui-disabled": { backgroundColor: "#EDF6FC" },
        },
        notchedOutline: { borderColor: "#C9DDEB" },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          border: "1px solid rgba(215,229,239,0.94)",
          boxShadow:
            "inset 0 1px 0 rgba(255,255,255,0.9), 0 34px 70px -34px rgba(30,92,135,0.38), 0 18px 36px -24px rgba(16,42,67,0.22)",
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 18,
          border: "1px solid rgba(215,229,239,0.94)",
          boxShadow:
            "inset 0 1px 0 rgba(255,255,255,0.9), 0 34px 70px -34px rgba(30,92,135,0.38), 0 18px 36px -24px rgba(16,42,67,0.22)",
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: "#D7E5EF", fontVariantNumeric: "tabular-nums" },
        head: { color: "#31566F", backgroundColor: "rgba(237,246,252,0.82)", fontWeight: 700 },
      },
    },
    MuiTypography: {
      styleOverrides: {
        root: {
          wordBreak: "keep-all",
          overflowWrap: "break-word",
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: { border: "1px solid currentColor", borderColor: "color-mix(in srgb, currentColor 22%, transparent)" },
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
