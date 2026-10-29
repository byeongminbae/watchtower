import { BlockList, isIP } from "node:net"

export type LookupAll = (host: string) => Promise<string[]>

const internalAddresses = new BlockList()
internalAddresses.addSubnet("0.0.0.0", 8, "ipv4")
internalAddresses.addSubnet("10.0.0.0", 8, "ipv4")
internalAddresses.addSubnet("100.64.0.0", 10, "ipv4")
internalAddresses.addSubnet("127.0.0.0", 8, "ipv4")
internalAddresses.addSubnet("169.254.0.0", 16, "ipv4")
internalAddresses.addSubnet("172.16.0.0", 12, "ipv4")
internalAddresses.addSubnet("192.168.0.0", 16, "ipv4")
internalAddresses.addSubnet("224.0.0.0", 4, "ipv4")
internalAddresses.addAddress("::", "ipv6")
internalAddresses.addAddress("::1", "ipv6")
internalAddresses.addSubnet("fc00::", 7, "ipv6")
internalAddresses.addSubnet("fe80::", 10, "ipv6")
internalAddresses.addSubnet("fec0::", 10, "ipv6")
internalAddresses.addSubnet("ff00::", 8, "ipv6")

/** IP 형식이 아니면 판단할 수 없으므로 내부 주소로 취급함 */
export function isInternalAddress(address: string): boolean {
  const ipv4 = toIpv4IfMapped(address)
  if (ipv4 !== undefined) {
    return internalAddresses.check(ipv4, "ipv4")
  }

  const version = isIP(address)
  if (version === 0) {
    return true
  }
  return internalAddresses.check(address, version === 4 ? "ipv4" : "ipv6")
}

/** ::ffff:127.0.0.1 처럼 IPv6 형식으로 감싼 IPv4 주소를 꺼냄 */
function toIpv4IfMapped(address: string): string | undefined {
  const mappedPrefix = "::ffff:"
  if (!address.toLowerCase().startsWith(mappedPrefix)) {
    return undefined
  }
  const ipv4 = address.slice(mappedPrefix.length)
  return isIP(ipv4) === 4 ? ipv4 : undefined
}

/**
 * 크롤링 한 건 동안 접근할 호스트가 외부 주소인지 검사하는 함수를 만듦. 같은 호스트는 한 번만 조회함
 *
 * 조회한 주소와 브라우저가 실제로 접속하는 주소가 같다는 보장이 없어서 DNS rebinding 을 막지는 못함
 * 크롤러 서버를 내부망과 분리하는 것이 1차 방어이고, 이 검사는 그 설정이 잘못됐을 때를 대비한 보조 방어임
 */
export function createHostGuard(lookupAll: LookupAll): (host: string) => Promise<boolean> {
  const verdicts = new Map<string, Promise<boolean>>()

  return function isAllowedHost(host: string): Promise<boolean> {
    let verdict = verdicts.get(host)
    if (verdict === undefined) {
      verdict = resolvesOnlyToExternalAddresses(lookupAll, host)
      verdicts.set(host, verdict)
    }
    return verdict
  }
}

async function resolvesOnlyToExternalAddresses(lookupAll: LookupAll, host: string): Promise<boolean> {
  const addresses = await lookupAll(toBareHost(host)).catch(() => [])
  return addresses.length > 0 && addresses.every((address) => !isInternalAddress(address))
}

/** URL 의 IPv6 호스트는 [::1] 처럼 대괄호로 감싸져 있음 */
function toBareHost(host: string): string {
  return host.startsWith("[") && host.endsWith("]") ? host.slice(1, -1) : host
}
