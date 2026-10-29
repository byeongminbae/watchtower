export interface ResultQueue {
  /** 전부 넣거나 전부 실패함 */
  publishAll(contents: string[]): Promise<void>
}
