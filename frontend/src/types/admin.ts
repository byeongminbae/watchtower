import type { Member, MemberRole } from "./domain";

export type AdminMember = Member & {
  readonly role: MemberRole;
  readonly bannedAt: string | null;
  readonly banReason: string | null;
  readonly deletedAt: string | null;
};
export type TowerStats = {
  readonly totalCount: number;
  readonly runningCount: number;
  readonly pausedCount: number;
  readonly illegalSuspendedCount: number;
};
export type MemberStats = {
  readonly totalCount: number;
  readonly activeCount: number;
  readonly bannedCount: number;
  readonly deletedCount: number;
};
export type AuthStats = {
  readonly signUps: { readonly last7Days: number; readonly last30Days: number };
  readonly signIns: { readonly last7Days: number; readonly last30Days: number };
};
