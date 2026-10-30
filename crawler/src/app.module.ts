import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ConfigLoader } from './config/config.js';
import { CrawlModule } from './crawl/crawl.module.js';

/**
 * 설정은 validate 대신 load 로 등록함
 * validate 는 검증 결과의 문자열 값을 process.env 에 다시 써넣어 OCI 개인키가 다른 이름으로 복제됨
 */
@Module({
  imports: [ConfigModule.forRoot({ isGlobal: true, load: [() => ConfigLoader.load(process.env)] }), CrawlModule]
})
export class AppModule {}
