import { promisify } from 'node:util';
import { gzip } from 'node:zlib';
import type { ObjectStorageClient } from 'oci-objectstorage';
import type { HtmlStorage } from '../../crawl/interfaces/html-storage.interface.js';

const gzipAsync = promisify(gzip);

/** HTML 을 gzip 으로 압축해 저장함. Content-Encoding 을 gzip 으로 붙여 읽는 쪽이 압축 여부를 알 수 있게 함 */
export class OciHtmlStorage implements HtmlStorage {
  constructor(
    private readonly objectStorageClient: ObjectStorageClient,
    private readonly namespaceName: string
  ) {}

  async saveHtml(bucketName: string, objectName: string, html: string): Promise<void> {
    await this.objectStorageClient.putObject({
      namespaceName: this.namespaceName,
      bucketName,
      objectName,
      putObjectBody: await gzipAsync(html),
      contentType: 'text/html; charset=utf-8',
      contentEncoding: 'gzip'
    });
  }
}
