export function isRouteOf(pathname: string, route: string): boolean {
  return pathname === route || pathname.startsWith(`${route}/`);
}
