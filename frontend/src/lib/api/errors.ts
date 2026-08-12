export class ApiError extends Error {
  readonly name: string = "ApiError";

  constructor(
    message: string,
    readonly status: number,
    readonly code: string | undefined = undefined,
    readonly raw: unknown = undefined,
  ) {
    super(message);
  }
}

export class BackendFeatureUnavailableError extends ApiError {
  readonly name: string = "BackendFeatureUnavailableError";

  constructor(readonly feature: string) {
    super("백엔드 기능이 아직 제공되지 않습니다.", 501, "BACKEND_FEATURE_UNAVAILABLE", { feature });
  }
}
