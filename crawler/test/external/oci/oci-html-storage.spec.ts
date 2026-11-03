import { gunzipSync } from 'node:zlib';
import type { ObjectStorageClient } from 'oci-objectstorage';
import { OciHtmlStorage } from '../../../src/external/oci/oci-html-storage.js';

describe('OciHtmlStorage', () => {
  it('HTML 을 gzip 으로 압축하고 압축했다는 표시를 붙여 저장함', async () => {
    // given
    const putObject = vi.fn().mockResolvedValue({});
    const storage = new OciHtmlStorage({ putObject } as unknown as ObjectStorageClient, 'namespace');
    const html = '<html><body>재입고 알림</body></html>';

    // when
    await storage.saveHtml('bucket', 'towers/7/2026-11-04T01:00:00.000Z.html', html);

    // then
    const request = putObject.mock.calls[0]?.[0];
    expect(request).toMatchObject({
      namespaceName: 'namespace',
      bucketName: 'bucket',
      objectName: 'towers/7/2026-11-04T01:00:00.000Z.html',
      contentType: 'text/html; charset=utf-8',
      contentEncoding: 'gzip'
    });
    expect(gunzipSync(request.putObjectBody).toString('utf8')).toBe(html);
  });
});
