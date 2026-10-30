export abstract class HtmlStorage {
  abstract saveHtml(bucketName: string, objectName: string, html: string): Promise<void>;
}
