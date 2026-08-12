import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  ApiError,
  BackendFeatureUnavailableError,
} from "@/lib/api/errors";
import { useApiData } from "@/lib/useApiData";

describe("useApiData unavailable state", () => {
  it("Given an unavailable backend feature, when fetched, then surfaces the stable Korean unavailable state", async () => {
    // Given
    const fetcher = () => Promise.reject(new BackendFeatureUnavailableError("admin.getPayments"));

    // When
    const { result } = renderHook(() => useApiData(fetcher));

    // Then
    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.data).toBeNull();
    expect(result.current.error).toBe("백엔드 기능이 아직 제공되지 않습니다.");
  });

  it("Given stale fake UI data and an unavailable refetch, when refetched, then clears stale data", async () => {
    // Given
    let available = true;
    const fetcher = () =>
      available
        ? Promise.resolve(["fixture"])
        : Promise.reject(new BackendFeatureUnavailableError("admin.getPayments"));
    const { result } = renderHook(() => useApiData(fetcher));
    await waitFor(() => {
      expect(result.current.data).toEqual(["fixture"]);
    });

    // When
    available = false;
    act(() => result.current.refetch());

    // Then
    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.data).toBeNull();
    expect(result.current.error).toBe("백엔드 기능이 아직 제공되지 않습니다.");
  });

  it("Given an untrusted backend message, when fetching fails, then never renders that external text", async () => {
    // Given
    const externalMessage = "<script>fixture backend message</script>";
    const fetcher = () =>
      Promise.reject(
        new ApiError(externalMessage, 500, "EXTERNAL_FAILURE"),
      );

    // When
    const { result } = renderHook(() => useApiData(fetcher));

    // Then
    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe("현재는 데이터를 불러올 수 없습니다");
    expect(result.current.error).not.toContain(externalMessage);
  });
});
