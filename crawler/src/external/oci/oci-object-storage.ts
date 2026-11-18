import type { ObjectStorageClient } from 'oci-objectstorage';
import type { SnapshotStorage } from '../../crawl/interfaces/snapshot-storage.js';

export class OciObjectStorage implements SnapshotStorage {
  constructor(
    private readonly objectStorageClient: ObjectStorageClient,
    private readonly namespaceName: string,
    private readonly bucketName: string
  ) {}

  async saveText(objectName: string, text: string): Promise<void> {
    await this.objectStorageClient.putObject({
      namespaceName: this.namespaceName,
      bucketName: this.bucketName,
      objectName,
      putObjectBody: Buffer.from(text, 'utf-8'),
      contentType: 'text/plain; charset=utf-8'
    });
  }
}
