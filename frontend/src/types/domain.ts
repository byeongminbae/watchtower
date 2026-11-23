export type MemberRole = "NORMAL" | "ADMIN";

export interface Member {
  readonly id: number;
  readonly nickname: string;
  readonly profileImageUrl: string;
  readonly lastSignInAt: string;
}
export type * from "./tower";
