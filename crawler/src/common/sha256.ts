import { hash } from 'node:crypto';

export function sha256(content: string | Buffer): string {
  return hash('sha256', content);
}
