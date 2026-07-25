// 백엔드는 SuccessResponse<T> = { data, timestamp } 형태로 응답한다.
// 에러 응답 스펙은 스웨거에 명시되어 있지 않아, 기존 코드베이스의 ErrorResponse 컨벤션을 따른다고 추론한다:
// { error: { code, message }, timestamp }
export class ApiError extends Error {
  status: number;
  code?: string;
  raw?: unknown;

  constructor(message: string, status: number, code?: string, raw?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.raw = raw;
  }
}

export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
  };
  timestamp?: string;
}
