/**
 * 여러 큐의 수신 루프가 함께 쓰는 동시 처리 한도
 *
 * 자리가 나면 기다리던 acquire 에 바로 넘겨서, 자리가 비었다고 알림을 받은 쪽끼리 다투지 않게 함
 */
export class ConcurrencyLimiter {
  private available: number
  private readonly acquirers: Array<() => void> = []
  private readonly observers: Array<() => void> = []

  constructor(limit: number) {
    this.available = limit
  }

  get availableSlots(): number {
    return this.available
  }

  /** @return 자리를 돌려주는 함수. 여러 번 호출해도 한 번만 돌려줌 */
  async acquire(): Promise<() => void> {
    if (this.available > 0) {
      this.available--
    } else {
      await new Promise<void>((resolve) => this.acquirers.push(resolve))
    }

    let isReleased = false
    return () => {
      if (!isReleased) {
        isReleased = true
        this.release()
      }
    }
  }

  /** 자리를 차지하지 않고 빈 자리가 생길 때까지만 기다림 */
  async whenAvailable(): Promise<void> {
    if (this.available > 0) {
      return
    }
    await new Promise<void>((resolve) => this.observers.push(resolve))
  }

  private release(): void {
    const nextAcquirer = this.acquirers.shift()
    if (nextAcquirer !== undefined) {
      nextAcquirer()
      return
    }

    this.available++
    this.observers.splice(0).forEach((notify) => notify())
  }
}
