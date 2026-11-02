import { Injectable, type OnModuleInit } from '@nestjs/common';

/**
 * 크롤러가 시작할 때 컨테이너가 내부망(인스턴스 메타데이터)에 닿지 못하는지 확인하고, 닿으면 기동을 멈춤
 *
 * 크롤러는 사용자가 입력한 URL 을 열고, CrawlHostValidator 는 DNS rebinding 까지 막지 못해서 호스트 방화벽(infra/crawler-firewall.sh)이 마지막 방어선임
 * onModuleInit 은 CrawlRequestConsumer 가 큐 수신을 시작하는 onApplicationBootstrap 보다 먼저 끝나므로, 검사를 통과하기 전에는 요청을 하나도 받지 않음
 * 배포뿐 아니라 재시작이나 호스트 재부팅으로 방화벽이 빠진 채 다시 시작되는 경우도 막음. 기동을 멈추면 배포 헬스체크가 실패해 이전 크롤러가 계속 돎
 */
@Injectable()
export class InternalNetworkIsolationCheck implements OnModuleInit {
  private static readonly INSTANCE_METADATA_URL = 'http://169.254.169.254/opc/v2/instance/';
  private static readonly TIMEOUT_MILLIS = 5_000;

  async onModuleInit(): Promise<void> {
    if (await InternalNetworkIsolationCheck.isInstanceMetadataReachable()) {
      throw new Error(
        '크롤러 컨테이너가 인스턴스 메타데이터에 접근할 수 있어 기동을 멈춤. 크롤러 호스트에 infra/crawler-firewall.sh 를 적용해야 함'
      );
    }
  }

  /** 응답 코드와 상관없이 응답을 받았으면 닿은 것이고, 연결이 거부되거나 시간 안에 연결되지 않으면 차단된 것임 */
  private static async isInstanceMetadataReachable(): Promise<boolean> {
    try {
      await fetch(InternalNetworkIsolationCheck.INSTANCE_METADATA_URL, {
        headers: { Authorization: 'Bearer Oracle' },
        signal: AbortSignal.timeout(InternalNetworkIsolationCheck.TIMEOUT_MILLIS)
      });
      return true;
    } catch {
      return false;
    }
  }
}
