import { describe, expect, it } from "vitest"
import { ConcurrencyLimiter } from "../../src/crawl/concurrency-limiter.ts"

describe("ConcurrencyLimiter", () => {
  it("한도가 차면 자리가 날 때까지 기다리고, 돌려준 자리는 기다리던 쪽이 바로 가져감", async () => {
    // given
    const limiter = new ConcurrencyLimiter(1)
    const releaseFirst = await limiter.acquire()
    let isSecondAcquired = false
    const second = limiter.acquire().then((release) => {
      isSecondAcquired = true
      return release
    })

    // when
    await Promise.resolve()
    const acquiredBeforeRelease = isSecondAcquired
    releaseFirst()
    await second

    // then
    expect(acquiredBeforeRelease).toBe(false)
    expect(isSecondAcquired).toBe(true)
    expect(limiter.availableSlots).toBe(0)
  })

  it("같은 자리를 여러 번 돌려줘도 한 번만 반영함", async () => {
    // given
    const limiter = new ConcurrencyLimiter(2)
    const release = await limiter.acquire()

    // when
    release()
    release()

    // then
    expect(limiter.availableSlots).toBe(2)
  })

  it("빈 자리를 기다리기만 하는 쪽은 자리를 차지하지 않음", async () => {
    // given
    const limiter = new ConcurrencyLimiter(1)
    const release = await limiter.acquire()
    const waiting = limiter.whenAvailable()

    // when
    release()
    await waiting

    // then
    expect(limiter.availableSlots).toBe(1)
  })
})
