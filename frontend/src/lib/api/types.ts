import { ApiError } from "./errors";

export interface ApiEnvelope<T> {
  readonly success: true;
  readonly data: T;
  readonly timestamp: string;
}

export interface ApiEmptyEnvelope {
  readonly success: true;
  readonly timestamp: string;
}

export type ApiDataGuard<T> = (value: unknown) => value is T;

export type SuccessEnvelopeRecord = Record<string, unknown> & {
  readonly success: true;
  readonly timestamp: string;
};

export type ResponseDecoder<T> = (
  value: SuccessEnvelopeRecord,
  status: number,
) => T;

export function invalidResponse(value: unknown, status: number): ApiError {
  return new ApiError(
    "API 응답 데이터 형식이 올바르지 않습니다.",
    status,
    "INVALID_RESPONSE",
    value,
  );
}

export function isSuccessEnvelope(value: unknown): value is SuccessEnvelopeRecord {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    "success" in value &&
    value.success === true &&
    "timestamp" in value &&
    typeof value.timestamp === "string"
  );
}

export function decodeUnknownData(
  value: SuccessEnvelopeRecord,
  status: number,
): ApiEnvelope<unknown> {
  if (!Object.hasOwn(value, "data")) throw invalidResponse(value, status);
  return { success: true, data: value.data, timestamp: value.timestamp };
}

export function decodeGuardedData<T>(
  value: SuccessEnvelopeRecord,
  status: number,
  guard: ApiDataGuard<T>,
): ApiEnvelope<T> {
  if (!Object.hasOwn(value, "data") || !guard(value.data)) {
    throw invalidResponse(value, status);
  }
  return { success: true, data: value.data, timestamp: value.timestamp };
}

export function decodeEmptyData(
  value: SuccessEnvelopeRecord,
  status: number,
): ApiEmptyEnvelope {
  if (Object.hasOwn(value, "data")) throw invalidResponse(value, status);
  return { success: true, timestamp: value.timestamp };
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
