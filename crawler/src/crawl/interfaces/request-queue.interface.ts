export type ReceivedMessage = {
  content: string;
  receipt: string;
  deliveryCount: number;
};

export interface RequestQueue {
  /** 메시지가 없으면 최대 폴링 대기 시간만큼 기다린 뒤 빈 배열을 돌려줌 */
  receive(limit: number): Promise<ReceivedMessage[]>;

  /** 지우지 못한 메시지는 visibility 시간이 지나면 다시 전달됨 */
  completeAll(receipts: string[]): Promise<void>;
}
