export interface ApiEnvelope<T> {
  readonly data: T;
  readonly timestamp: string;
}

export class InvalidApiDataError extends Error {
  constructor() {
    super("API 응답 데이터 형식이 올바르지 않습니다.");
    this.name = "InvalidApiDataError";
  }
}

export function requireArrayData<T>(response: ApiEnvelope<T[]>): ApiEnvelope<T[]> {
  if (!Array.isArray(response.data)) {
    throw new InvalidApiDataError();
  }
  return response;
}

export function requireObjectData<T extends object>(response: ApiEnvelope<T>): ApiEnvelope<T> {
  if (typeof response.data !== "object" || response.data === null || Array.isArray(response.data)) {
    throw new InvalidApiDataError();
  }
  return response;
}
