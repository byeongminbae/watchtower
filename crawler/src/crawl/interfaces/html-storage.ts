export const HTML_STORAGE = Symbol("HtmlStorage")

export interface HtmlStorage {
  saveHtml(bucketName: string, objectName: string, html: string): Promise<void>
}
