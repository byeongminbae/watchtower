export type NotificationChannelType = "DISCORD" | "TELEGRAM";

export interface NotificationChannel {
  readonly id: number;
  readonly name: string;
  readonly type: NotificationChannelType;
  readonly webhookUrl: string;
}

export interface NotificationChannelPage {
  readonly data: NotificationChannel[];
  readonly size: number;
  readonly nextCursorId: number | null;
  readonly nextCursorValue: string | null;
  readonly hasNext: boolean;
}

export type NotificationChannelUpdate = Pick<NotificationChannel, "name" | "webhookUrl">;
export type NotificationChannelCreate = NotificationChannelUpdate & Pick<NotificationChannel, "type">;
