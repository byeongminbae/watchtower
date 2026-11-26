import type { ObjectStorageClient } from 'oci-objectstorage';
import { OciObjectStorage } from '../../../src/external/oci/oci-object-storage.js';

describe('OciObjectStorage', () => {
  it('텍스트를 압축하지 않고 UTF-8 평문으로 저장함', async () => {
    // given
    const putObject = vi.fn().mockResolvedValue({});
    const storage = new OciObjectStorage({ putObject } as unknown as ObjectStorageClient, 'namespace', 'bucket');
    const text = '재입고 알림';

    // when
    await storage.saveText('towers/7/2026-11-04T01:00:00.000Z.txt', text);

    // then
    const request = putObject.mock.calls[0]?.[0];
    expect(request).toMatchObject({
      namespaceName: 'namespace',
      bucketName: 'bucket',
      objectName: 'towers/7/2026-11-04T01:00:00.000Z.txt',
      contentType: 'text/plain; charset=utf-8'
    });
    expect(request).not.toHaveProperty('contentEncoding');
    expect(Buffer.from(request.putObjectBody).toString('utf8')).toBe(text);
  });

  it('스크린샷을 PNG 형식으로 저장함', async () => {
    // given
    const putObject = vi.fn().mockResolvedValue({});
    const storage = new OciObjectStorage({ putObject } as unknown as ObjectStorageClient, 'namespace', 'bucket');
    const screenshot = Buffer.from('PNG 이미지');

    // when
    await storage.saveScreenshot('towers/7/2026-11-04T01:00:00.000Z.png', screenshot);

    // then
    expect(putObject).toHaveBeenCalledWith({
      namespaceName: 'namespace',
      bucketName: 'bucket',
      objectName: 'towers/7/2026-11-04T01:00:00.000Z.png',
      putObjectBody: screenshot,
      contentType: 'image/png'
    });
  });
});
