import { lookup } from "node:dns/promises"
import type { LookupAll } from "../../crawl/address-policy.ts"

/** 조회에 실패하면 예외를 그대로 던짐. 실패를 어떻게 다룰지는 호출하는 쪽이 정함 */
export const lookupAll: LookupAll = async function (host) {
  const addresses = await lookup(host, { all: true, verbatim: true })
  return addresses.map(({ address }) => address)
}
