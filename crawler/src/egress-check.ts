/**
 * 배포 workflow 가 크롤러 컨테이너 안에서 실행함. 인스턴스 메타데이터에 닿으면 호스트 방화벽이 빠진 것이므로 실패로 끝남
 *
 * 방화벽 설정은 infra/crawler-firewall.sh 에 있음
 */
try {
  await fetch('http://169.254.169.254/opc/v2/instance/', {
    headers: { Authorization: 'Bearer Oracle' },
    signal: AbortSignal.timeout(5_000)
  });
  console.error('크롤러 컨테이너가 인스턴스 메타데이터에 접근할 수 있음. infra/crawler-firewall.sh 를 적용해야 함');
  process.exitCode = 1;
} catch {
  console.info('크롤러 컨테이너의 내부망 접근이 차단되어 있음');
}
