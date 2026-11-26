import type { ObjectStorageClient } from 'oci-objectstorage';
import type { PageContentStorage } from '../../crawl/interfaces/page-content-storage.js';

export class OciObjectStorage implements PageContentStorage {
  constructor(
    private readonly objectStorageClient: ObjectStorageClient,
    private readonly namespaceName: string,
    private readonly bucketName: string
  ) {}

  async saveText(objectName: string, text: string): Promise<void> {
    await this.putObject(objectName, Buffer.from(text, 'utf-8'), 'text/plain; charset=utf-8');
  }

  async saveScreenshot(objectName: string, screenshot: Buffer): Promise<void> {
    await this.putObject(objectName, screenshot, 'image/png');
  }

  private async putObject(objectName: string, body: Buffer, contentType: string): Promise<void> {
    await this.objectStorageClient.putObject({
      namespaceName: this.namespaceName,
      bucketName: this.bucketName,
      objectName,
      putObjectBody: body,
      contentType
    });
  }
}
