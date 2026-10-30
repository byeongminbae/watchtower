import type { ObjectStorageClient } from 'oci-objectstorage';
import { HtmlStorage } from '../../crawl/interfaces/html-storage.interface.js';

export class OciHtmlStorage extends HtmlStorage {
  constructor(
    private readonly client: ObjectStorageClient,
    private readonly namespaceName: string
  ) {
    super();
  }

  override async saveHtml(bucketName: string, objectName: string, html: string): Promise<void> {
    await this.client.putObject({
      namespaceName: this.namespaceName,
      bucketName,
      objectName,
      putObjectBody: html,
      contentType: 'text/html; charset=utf-8'
    });
  }
}
