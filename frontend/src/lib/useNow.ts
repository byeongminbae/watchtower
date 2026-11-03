"use client";

import { useSyncExternalStore } from "react";

function subscribe(onChange: () => void) {
  const timer = window.setInterval(onChange, 1_000);
  document.addEventListener("visibilitychange", onChange);
  return () => {
    window.clearInterval(timer);
    document.removeEventListener("visibilitychange", onChange);
  };
}

function currentSecond() {
  return Math.floor(Date.now() / 1_000) * 1_000;
}

/**
 * 1초마다 갱신되는 현재 시각(밀리초). 서버 렌더링 중에는 null
 *
 * 백그라운드 탭에서는 브라우저가 타이머를 늦추므로, 탭이 다시 보이는 순간에도 바로 갱신함
 */
export function useNow(): number | null {
  return useSyncExternalStore<number | null>(subscribe, currentSecond, () => null);
}
