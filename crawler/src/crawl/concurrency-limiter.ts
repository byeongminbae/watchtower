/**
 * 여러 큐의 수신 루프가 함께 쓰는 동시 처리 한도
 *
 * 자리가 나면 기다리던 acquire 에 바로 넘겨서, 자리가 비었다고 알림을 받은 쪽끼리 다투지 않게 함
 */
export class ConcurrencyLimiter {
  private available: number;
  private readonly acquirers: Array<() => void> = [];
  private readonly observers: Array<() => void> = [];

  constructor(limit: number) {
    this.available = limit;
  }

  get availableSlots(): number {
    return this.available;
  }

  /** @return 자리를 돌려주는 함수 */
  async acquire(): Promise<() => void> {
    if (this.hasAvailableSlot()) {
      this.available--;
    } else {
      await new Promise<void>((resolve) => this.acquirers.push(resolve));
    }
    return this.releaseOnce();
  }

  /** 자리를 차지하지 않고 빈 자리가 생길 때까지만 기다림 */
  async whenAvailable(): Promise<void> {
    if (this.hasAvailableSlot()) {
      return;
    }
    await new Promise<void>((resolve) => this.observers.push(resolve));
  }

  private hasAvailableSlot(): boolean {
    return this.available > 0;
  }

  private releaseOnce(): () => void {
    let isReleased = false;
    return () => {
      if (!isReleased) {
        isReleased = true;
        this.release();
      }
    };
  }

  private release(): void {
    if (this.handOverToNextAcquirer()) {
      return;
    }
    this.available++;
    this.notifyObservers();
  }

  private handOverToNextAcquirer(): boolean {
    const nextAcquirer = this.acquirers.shift();
    nextAcquirer?.();
    return nextAcquirer !== undefined;
  }

  private notifyObservers(): void {
    this.observers.splice(0).forEach((notify) => notify());
  }
}
