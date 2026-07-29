const STORAGE_KEY = "watchtower_oauth_transaction";
const TRANSACTION_TTL_MS = 10 * 60 * 1_000;
const FALLBACK_RETURN_PATH = "/watches";
const CONTROL_CHARACTER = /[\u0000-\u001f\u007f]/u;

type StoredOAuthTransaction = {
  readonly nonce: string;
  readonly returnPath: string;
  readonly createdAt: number;
};

function isSafeLocalPath(path: string): boolean {
  return (
    path.startsWith("/") &&
    !path.startsWith("//") &&
    !path.includes("\\") &&
    !CONTROL_CHARACTER.test(path)
  );
}

export function sanitizeReturnPath(path: string): string {
  if (!isSafeLocalPath(path)) return FALLBACK_RETURN_PATH;

  let decoded = path;
  while (true) {
    let next: string;
    try {
      next = decodeURIComponent(decoded);
    } catch (error) {
      if (error instanceof URIError) return FALLBACK_RETURN_PATH;
      throw error;
    }

    if (!isSafeLocalPath(next)) return FALLBACK_RETURN_PATH;
    if (next === decoded) return path;
    decoded = next;
  }
}

export function createOAuthTransaction(returnPath: string): string {
  const transaction: StoredOAuthTransaction = {
    nonce: crypto.randomUUID(),
    returnPath: sanitizeReturnPath(returnPath),
    createdAt: Date.now(),
  };
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(transaction));
  return transaction.nonce;
}

export function consumeOAuthTransaction(state: string): string | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
  if (raw === null) return null;

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    if (error instanceof SyntaxError) return null;
    throw error;
  }

  if (
    typeof parsed !== "object" ||
    parsed === null ||
    !("nonce" in parsed) ||
    typeof parsed.nonce !== "string" ||
    !("returnPath" in parsed) ||
    typeof parsed.returnPath !== "string" ||
    !("createdAt" in parsed) ||
    typeof parsed.createdAt !== "number"
  ) {
    return null;
  }

  const ageMs = Date.now() - parsed.createdAt;
  if (
    parsed.nonce !== state ||
    !Number.isFinite(ageMs) ||
    ageMs < 0 ||
    ageMs > TRANSACTION_TTL_MS
  ) {
    return null;
  }

  return sanitizeReturnPath(parsed.returnPath);
}

export function clearOAuthTransaction(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}
