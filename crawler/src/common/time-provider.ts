import { Injectable } from '@nestjs/common';

/** 테스트에서 시각을 고정할 수 있도록 현재 시각은 이 클래스를 주입받아 구함 */
@Injectable()
export class TimeProvider {
  now(): Date {
    return new Date();
  }
}
