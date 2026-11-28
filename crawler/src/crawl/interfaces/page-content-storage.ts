export interface PageContentStorage {
  saveText(objectName: string, text: string): Promise<void>;
  saveScreenshot(objectName: string, screenshot: Buffer): Promise<void>;
}
