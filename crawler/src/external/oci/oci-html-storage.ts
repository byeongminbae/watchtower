import type { ObjectStorageClient } from "oci-objectstorage"
import type { HtmlStorage } from "../../crawl/interfaces/html-storage.ts"

export class OciHtmlStorage implements HtmlStorage {
  constructor(
    private readonly client: ObjectStorageClient,
    private readonly namespaceName: string
  ) {}

  async saveHtml(bucketName: string, objectName: string, html: string): Promise<void> {
    await this.client.putObject({
      namespaceName: this.namespaceName,
      bucketName,
      objectName,
      putObjectBody: html,
      contentType: "text/html; charset=utf-8"
    })
  }
}
