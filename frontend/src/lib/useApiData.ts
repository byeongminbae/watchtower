"use client";

import React from "react";
import { ApiError } from "@/lib/api/errors";

interface UseApiDataResult<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

// 컴포넌트 마운트 시 자동으로 fetcher를 호출하고 loading/error 상태를 관리하는 훅.
// deps가 바뀌면 재요청한다.
//
// loading 여부는 별도 boolean state를 effect 안에서 동기적으로 세팅하는 대신,
// "요청 세대(generation) 번호"와 "마지막으로 완료된 세대 번호"를 비교해서 파생한다.
// (effect 진입 시 setState를 동기 호출하면 캐스케이딩 렌더를 유발하므로 지양)
export function useApiData<T>(fetcher: () => Promise<T>, deps: React.DependencyList = []): UseApiDataResult<T> {
  const [data, setData] = React.useState<T | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [pendingGeneration, setPendingGeneration] = React.useState(0);
  const [resolvedGeneration, setResolvedGeneration] = React.useState(0);
  const generationRef = React.useRef(0);
  const fetcherRef = React.useRef(fetcher);

  React.useLayoutEffect(() => {
    fetcherRef.current = fetcher;
  });

  const runFetch = React.useCallback(() => {
    const currentGeneration = ++generationRef.current;
    setPendingGeneration(currentGeneration);

    fetcherRef
      .current()
      .then((result) => {
        if (generationRef.current !== currentGeneration) return;
        setData(result);
        setError(null);
        setResolvedGeneration(currentGeneration);
      })
      .catch((err) => {
        if (generationRef.current !== currentGeneration) return;
        setError(err instanceof ApiError ? err.message : "데이터를 불러오지 못했습니다.");
        setResolvedGeneration(currentGeneration);
      });
  }, []);

  React.useEffect(() => {
    runFetch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return {
    data,
    error,
    loading: pendingGeneration !== resolvedGeneration,
    refetch: runFetch,
  };
}
