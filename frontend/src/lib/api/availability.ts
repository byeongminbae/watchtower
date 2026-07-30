import { BackendFeatureUnavailableError } from "./errors";

type BackendContractClass =
  | "callable"
  | "placeholder-data"
  | "empty-success"
  | "absent";

type BackendFeatureEntry = {
  readonly contractClass: BackendContractClass;
};

export const backendFeatureInventory = {
  "auth.getNaverLoginUrl": { contractClass: "callable" },
  "auth.loginWithNaverCallback": { contractClass: "callable" },
  "auth.revokeNaverToken": { contractClass: "empty-success" },
  "auth.renewSession": { contractClass: "callable" },
  "auth.logout": { contractClass: "callable" },
  "member.getMember": { contractClass: "callable" },
  "member.updateMember": { contractClass: "empty-success" },
  "member.deleteMember": { contractClass: "empty-success" },
  "member.getMemberWatches": { contractClass: "placeholder-data" },
  "member.getMemberPaymentHistories": { contractClass: "placeholder-data" },
  "member.getMemberSubscription": { contractClass: "placeholder-data" },
  "member.cancelMemberSubscription": { contractClass: "empty-success" },
  "watch.createWatch": { contractClass: "empty-success" },
  "watch.getTrendingWatches": { contractClass: "placeholder-data" },
  "watch.getWatch": { contractClass: "placeholder-data" },
  "watch.updateWatch": { contractClass: "empty-success" },
  "watch.deleteWatch": { contractClass: "empty-success" },
  "watch.sendTestNotification": { contractClass: "empty-success" },
  "watch.getWatchConditions": { contractClass: "placeholder-data" },
  "watch.createWatchCondition": { contractClass: "empty-success" },
  "watch.updateWatchCondition": { contractClass: "empty-success" },
  "watch.deleteWatchCondition": { contractClass: "empty-success" },
  "watch.listWatchSnapshots": { contractClass: "placeholder-data" },
  "watch.getWatchSnapshot": { contractClass: "placeholder-data" },
  "payment.getPayment": { contractClass: "placeholder-data" },
  "payment.confirmTossPayment": { contractClass: "empty-success" },
  "admin.getWatches": { contractClass: "absent" },
  "admin.updateWatchStatus": { contractClass: "empty-success" },
  "admin.cancelPayment": { contractClass: "empty-success" },
  "admin.getPayments": { contractClass: "placeholder-data" },
  "admin.getUsers": { contractClass: "placeholder-data" },
  "admin.updateUserRole": { contractClass: "empty-success" },
  "admin.updateUserStatus": { contractClass: "empty-success" },
  "admin.getWatchStats": { contractClass: "placeholder-data" },
  "admin.getUserStats": { contractClass: "placeholder-data" },
  "admin.getPaymentStats": { contractClass: "placeholder-data" },
  "admin.getAuthStats": { contractClass: "placeholder-data" },
} as const satisfies Record<string, BackendFeatureEntry>;

export type BackendFeature = keyof typeof backendFeatureInventory;

type UnavailableBackendFeature = {
  readonly [Feature in BackendFeature]:
    (typeof backendFeatureInventory)[Feature]["contractClass"] extends "callable"
      ? never
      : Feature;
}[BackendFeature];

export const BACKEND_FEATURE_UNAVAILABLE_MESSAGE =
  "백엔드 기능이 아직 제공되지 않습니다.";

export function rejectUnavailable<T>(
  feature: UnavailableBackendFeature,
): Promise<T> {
  return Promise.reject(new BackendFeatureUnavailableError(feature));
}
