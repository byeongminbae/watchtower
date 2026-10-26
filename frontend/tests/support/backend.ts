import { createServer } from "node:http";
import type { Member } from "../../src/types/domain";
import type { Watch, WatchCondition } from "../../src/types/tower";
import type { NotificationChannel } from "../../src/types/notification";
import type { AdminMember } from "../../src/types/admin";

type State = {
  member: Member;
  towers: Watch[];
  channels: NotificationChannel[];
  conditions: Map<number, WatchCondition[]>;
  members: AdminMember[];
  records: { method: string; path: string; body: unknown; csrf: string | undefined }[];
};
const timestamp = "2026-10-26T12:00:00";
const states = new Map<string, State>();
const oauthStates = new Map<string, string>();
const invalidSessions = new Set<string>();
let sequence = 100;
function makeState(): State {
  const member = { id: 7, nickname: "테스트 회원", email: "member@example.com", profileImageUrl: "/watchtower.png", lastSignInAt: timestamp };
  const tower: Watch = { id: 9, name: "채용 공고", url: "https://example.com/jobs", fetchInterval: "PT5M", includeInStat: true,
    status: "RUNNING", faviconUrl: "/watchtower.png", lastFetchedAt: timestamp, lastNotifiedAt: timestamp, latestAiSummary: "새 공고가 등록되었어요." };
  return { member, towers: [tower], channels: [{ id: 3, name: "개발 알림", type: "DISCORD", webhookUrl: "https://example.com/hooks/test" }],
    conditions: new Map([[9, [{ id: 1, type: "HTML" }]]]), records: [],
    members: [
      { ...member, role: "NORMAL", bannedAt: null, banReason: null, deletedAt: null },
      { ...member, id: 8, nickname: "정지 회원", email: "banned@example.com", role: "NORMAL", bannedAt: timestamp, banReason: "정책 위반", deletedAt: null },
      { ...member, id: 10, nickname: "탈퇴 회원", email: "deleted@example.com", role: "NORMAL", bannedAt: null, banReason: null, deletedAt: timestamp }
    ] };
}
function success(data?: unknown) { return data === undefined ? { isSuccess: true, timestamp } : { isSuccess: true, timestamp, data }; }
function failure(code: string, message: string) { return { isSuccess: false, statusCode: code, message, timestamp }; }
function record(value: unknown): value is Record<string, unknown> { return typeof value === "object" && value !== null && !Array.isArray(value); }
function condition(body: Record<string, unknown>, id: number): WatchCondition | null {
  if (body.type === "HTML") return { id, type: "HTML" };
  if (body.type === "KEYWORD" && typeof body.keyword === "string" && typeof body.isSatisfiedAtAppearance === "boolean") {
    return { id, type: "KEYWORD", keyword: body.keyword, isSatisfiedAtAppearance: body.isSatisfiedAtAppearance };
  }
  if (body.type === "REGEX" && typeof body.regex === "string" && typeof body.isSatisfiedAtAppearance === "boolean") {
    return { id, type: "REGEX", regex: body.regex, isSatisfiedAtAppearance: body.isSatisfiedAtAppearance };
  }
  return null;
}
function page<T extends { readonly id: number }>(items: readonly T[], url: URL, offset: boolean) {
  const size = Number(url.searchParams.get("size") ?? 20);
  const index = Number(url.searchParams.get("page") ?? 0);
  const cursor = url.searchParams.get("cursorId");
  const direction = url.searchParams.get("sortDirection") ?? "DESC";
  const ordered = [...items].sort((a, b) => direction === "ASC" ? a.id - b.id : b.id - a.id);
  const start = offset ? index * size : cursor === null ? 0 : Math.max(0, ordered.findIndex(item => item.id === Number(cursor)) + 1);
  const data = ordered.slice(start, start + size);
  return offset ? { data, page: index, size, totalElements: items.length, totalPages: Math.ceil(items.length / size) } :
    { data, size, nextCursorId: data[data.length - 1]?.id ?? null, nextCursorValue: null, hasNext: start + size < items.length };
}
const server = createServer(async (request, response) => {
  const url = new URL(request.url ?? "/", "http://127.0.0.1:4100");
  const path = url.pathname.replace(/^\/api\/v1/, "");
  const method = request.method ?? "GET";
  const session = /(?:^|;\s*)JSESSIONID=([^;]+)/.exec(request.headers.cookie ?? "")?.[1] ?? "";
  let state = states.get(session);
  if (!state) { state = makeState(); states.set(session, state); }
  let raw = "";
  for await (const chunk of request) raw += String(chunk);
  let parsed: unknown = {};
  try { parsed = raw ? JSON.parse(raw) : {}; } catch (error) { if (!(error instanceof SyntaxError)) throw error; parsed = {}; }
  const body = record(parsed) ? parsed : {};
  const authenticated = !invalidSessions.has(session) && (session.startsWith("fixture-normal-") || session.startsWith("fixture-admin-"));
  const role = session.startsWith("fixture-admin-") ? "ADMIN" : "NORMAL";
  const csrfHeader = request.headers["x-csrf-token"];
  const csrf = typeof csrfHeader === "string" ? csrfHeader : undefined;
  response.setHeader("content-type", "application/json");
  const send = (data: unknown) => { response.end(JSON.stringify(data)); };
  if (path === "/health") { send(success()); return; }
  if (path === "/__test/invalidate") { invalidSessions.add(url.searchParams.get("session") ?? ""); send(success()); return; }
  if (path === "/__test/records") { send(success(states.get(url.searchParams.get("session") ?? "")?.records ?? [])); return; }
  state.records.push({ method, path: url.pathname, body, csrf });
  if (path === "/auth/csrf") {
    if (!session) response.setHeader("set-cookie", `JSESSIONID=fixture-guest-${++sequence}; Path=/; HttpOnly; SameSite=Lax`);
    send(success({ token: "csrf-fixture", headerName: "X-CSRF-TOKEN" })); return;
  }
  if ((method !== "GET" || path === "/auth/naver/url") && csrf !== "csrf-fixture") {
    send(failure("AUT_002", "CSRF 검증 실패")); return;
  }
  if (path === "/auth/naver/url") {
    oauthStates.set(session, url.searchParams.get("state") ?? "");
    send(success(`https://nid.naver.com/oauth2.0/authorize?state=${url.searchParams.get("state") ?? ""}`)); return;
  }
  if (path === "/auth/naver/callback") {
    const code = url.searchParams.get("code");
    const expected = oauthStates.get(session);
    if (code !== "fixture-code" || !expected || url.searchParams.get("state") !== expected) {
      send(failure("AUT_005", "잘못된 OAuth 요청")); return;
    }
    oauthStates.delete(session);
    const next = `fixture-normal-oauth-${++sequence}`;
    states.set(next, state);
    response.setHeader("set-cookie", `JSESSIONID=${next}; Path=/; HttpOnly; SameSite=Lax`);
    send(success({ memberId: 7, role: "NORMAL" })); return;
  }
  if (path === "/towers/trending") { send(success([{ domain: "example.com", url: "https://example.com/jobs", changeCount: 12 }])); return; }
  if (!authenticated) { send(failure("AUT_001", "인증이 필요해요.")); return; }
  if (path.startsWith("/admin/") && role !== "ADMIN") { send(failure("AUT_002", "권한이 부족해요.")); return; }
  if (path === "/auth/session") { send(success({ memberId: 7, role })); return; }
  if (path === "/auth/logout") {
    states.delete(session);
    response.setHeader("set-cookie", "JSESSIONID=; Max-Age=0; Path=/; HttpOnly");
    send(success()); return;
  }
  if (path === "/member/7") {
    if (method === "PUT" && typeof body.nickname === "string" && typeof body.profileImageUrl === "string") state.member = { ...state.member, nickname: body.nickname, profileImageUrl: body.profileImageUrl };
    if (method === "DELETE") { states.delete(session); response.setHeader("set-cookie", "JSESSIONID=; Max-Age=0; Path=/; HttpOnly"); send(success()); return; }
    send(success(state.member)); return;
  }
  const query = url.searchParams.get("query") ?? "";
  if (path === "/member/7/towers" || path === "/admin/towers") { send(success(page(state.towers.filter(tower => tower.name.startsWith(query)), url, path.startsWith("/admin/")))); return; }
  if (path === "/towers" && method === "POST") {
    if (typeof body.name !== "string" || typeof body.url !== "string" || typeof body.fetchInterval !== "string" || typeof body.includeInStat !== "boolean" || !Array.isArray(body.conditions)) { send(failure("COM_001", "타워 요청 오류")); return; }
    const id = ++sequence;
    state.towers.push({ id, name: body.name, url: body.url, fetchInterval: body.fetchInterval, includeInStat: body.includeInStat, status: "RUNNING", faviconUrl: null, lastFetchedAt: null, lastNotifiedAt: null });
    state.conditions.set(id, body.conditions.flatMap(item => { const result = record(item) ? condition(item, ++sequence) : null; return result ? [result] : []; }));
    send(success()); return;
  }
  const towerMatch = /^\/towers\/(\d+)(?:\/(conditions|snapshots)(?:\/(\d+))?)?$/.exec(path);
  if (towerMatch) {
    const id = Number(towerMatch[1]);
    const tower = state.towers.find(item => item.id === id);
    if (!tower) { send(failure("TOW_001", "타워가 없어요.")); return; }
    if (towerMatch[2] === "conditions") {
      const items = state.conditions.get(id) ?? [];
      const cid = Number(towerMatch[3]);
      if (method === "GET") { send(success(items)); return; }
      if (method === "DELETE") { state.conditions.set(id, items.filter(item => item.id !== cid)); send(success()); return; }
      const item = condition(body, cid || ++sequence);
      if (!item) { send(failure("COM_001", "조건 요청 오류")); return; }
      state.conditions.set(id, method === "PUT" ? items.map(old => old.id === cid ? item : old) : [...items, item]);
      send(method === "PUT" ? success(item) : success()); return;
    }
    if (towerMatch[2] === "snapshots") {
      send(success([{ id: 1, htmlContentUrl: "https://example.com/full.html", htmlContentDiffUrl: "https://example.com/diff.html", screenshotUrl: null, screenshotDiffUrl: null, aiDiffSummary: "신규 채용 공고", notifiedAt: timestamp }])); return;
    }
    if (method === "DELETE") { state.towers = state.towers.filter(item => item.id !== id); send(success()); return; }
    if (method === "PUT" && typeof body.name === "string" && typeof body.fetchInterval === "string" && typeof body.includeInStat === "boolean" && (body.status === "RUNNING" || body.status === "PAUSED")) {
      const updated: Watch = { ...tower, name: body.name, fetchInterval: body.fetchInterval, includeInStat: body.includeInStat, status: body.status };
      state.towers = state.towers.map(item => item.id === id ? updated : item); send(success(updated)); return;
    }
    send(success(tower)); return;
  }
  const channelMatch = /^\/notification\/channels(?:\/(\d+))?$/.exec(path);
  if (channelMatch) {
    const id = Number(channelMatch[1]);
    if (method === "GET") { send(success(id ? state.channels.find(item => item.id === id) : page(state.channels.filter(item => item.name.startsWith(query)), url, false))); return; }
    if (method === "DELETE") { state.channels = state.channels.filter(item => item.id !== id); send(success()); return; }
    const existing = state.channels.find(item => item.id === id);
    if (typeof body.name !== "string" || typeof body.webhookUrl !== "string" || (!existing && body.type !== "DISCORD" && body.type !== "TELEGRAM")) { send(failure("COM_001", "채널 요청 오류")); return; }
    const type = existing?.type ?? (body.type === "TELEGRAM" ? "TELEGRAM" : "DISCORD");
    const channel = { id: existing?.id ?? ++sequence, name: body.name, webhookUrl: body.webhookUrl, type };
    state.channels = existing ? state.channels.map(item => item.id === id ? channel : item) : [...state.channels, channel];
    send(success(channel)); return;
  }
  if (path === "/admin/members") { send(success(page(state.members.filter(item => item.nickname.startsWith(query) || item.email.startsWith(query)), url, true))); return; }
  const memberMatch = /^\/admin\/members\/(\d+)\/(roles|status)$/.exec(path);
  if (memberMatch) {
    const id = Number(memberMatch[1]);
    const member = state.members.find(item => item.id === id);
    if (!member || member.deletedAt) { send(failure("AUT_006", "탈퇴한 회원")); return; }
    const updated: AdminMember = memberMatch[2] === "roles" && (body.role === "NORMAL" || body.role === "ADMIN") ? { ...member, role: body.role } :
      { ...member, bannedAt: body.banned ? timestamp : null, banReason: body.banned && typeof body.banReason === "string" ? body.banReason : null };
    state.members = state.members.map(item => item.id === id ? updated : item); send(success(updated)); return;
  }
  const adminTower = /^\/admin\/towers\/(\d+)$/.exec(path);
  if (adminTower && (body.status === "RUNNING" || body.status === "PAUSED" || body.status === "ILLEGAL_SUSPENDED")) {
    const id = Number(adminTower[1]);
    const status = body.status;
    state.towers = state.towers.map(item => item.id === id ? { ...item, status } : item);
    send(success(state.towers.find(item => item.id === id))); return;
  }
  if (path === "/admin/stats/towers") { send(success({ totalCount: 1, runningCount: 1, pausedCount: 0, illegalSuspendedCount: 0 })); return; }
  if (path === "/admin/stats/members") { send(success({ totalCount: 3, activeCount: 1, bannedCount: 1, deletedCount: 1 })); return; }
  if (path === "/admin/stats/auth") { send(success({ signUps: { last7Days: 3, last30Days: 8 }, signIns: { last7Days: 12, last30Days: 40 } })); return; }
  response.statusCode = 404;
  send(failure("COM_404", "테스트 API 없음"));
});
server.listen(4100, "127.0.0.1");
process.on("SIGTERM", () => server.close());
