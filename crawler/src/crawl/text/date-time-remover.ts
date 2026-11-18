import RE2 from 're2';

const join = (...parts: string[]): string => parts.join('');
const optional = (...parts: string[]): string => `(?:${join(...parts)})?`;
const oneOf = (...choices: string[]): string => `(?:${choices.join('|')})`;

const SPACE = String.raw`[\s\v\p{Z}﻿]`;
const SPACES = `${SPACE}*`;

const YEAR = String.raw`\d{4}`;
const MONTH = String.raw`\d{1,2}`;
const DAY = String.raw`\d{1,2}`;
const HOUR = String.raw`\d{1,2}`;
const MINUTE = String.raw`\d{2}`;
const SECOND = String.raw`\d{2}`;
const FRACTION = String.raw`\.\d+`;
const TIME_ZONE = oneOf('Z', String.raw`[+-]\d{2}:?\d{2}`);
const AMOUNT = String.raw`\d+`;

const DATE_SEPARATOR = join(SPACES, '[-./]', SPACES);
const DATE_TIME_SEPARATOR = `${oneOf('T', SPACE)}+`;
const KOREAN_MERIDIEM = oneOf('오전', '오후');
const ENGLISH_MERIDIEM = String.raw`[AaPp]\.?[Mm]\.?`;
const KOREAN_WEEKDAY = String.raw`\(?[월화수목금토일](?:요일)?\)?`;

const NUMERIC_DATE_WITH_OPTIONAL_TIME = join(
  YEAR,
  DATE_SEPARATOR,
  MONTH,
  DATE_SEPARATOR,
  DAY,
  optional(String.raw`\.`),
  optional(DATE_TIME_SEPARATOR, HOUR, ':', MINUTE, optional(':', SECOND, optional(FRACTION)), optional(TIME_ZONE))
);
const KOREAN_DATE_WITH_OPTIONAL_WEEKDAY = join(
  optional(String.raw`\d{2,4}`, SPACES, '년', SPACES),
  MONTH,
  SPACES,
  '월',
  SPACES,
  DAY,
  SPACES,
  '일',
  optional(SPACES, KOREAN_WEEKDAY)
);
const CLOCK_TIME = join(
  optional(KOREAN_MERIDIEM),
  SPACES,
  HOUR,
  ':',
  MINUTE,
  optional(':', SECOND),
  optional(SPACES, ENGLISH_MERIDIEM)
);
const KOREAN_ELAPSED_TIME = join(
  AMOUNT,
  SPACES,
  oneOf('초', '분', '시간', '일', '주', '개월', '달', '년'),
  SPACES,
  '전'
);
const ENGLISH_ELAPSED_TIME = join(
  AMOUNT,
  SPACES,
  oneOf('seconds?', 'minutes?', 'mins?', 'hours?', 'days?', 'weeks?', 'months?', 'years?'),
  `${SPACE}+`,
  'ago'
);
const JUST_NOW = oneOf(join('방금', SPACES, '전'), 'just now');

/** 숫자만으로 된 표기(11.06 등)는 가격, 버전과 구분할 수 없어서 연도나 단위가 붙은 표기만 지움 */
export class DateTimeRemover {
  private static readonly PATTERNS: readonly RE2[] = [
    new RE2(NUMERIC_DATE_WITH_OPTIONAL_TIME, 'g'),
    new RE2(KOREAN_DATE_WITH_OPTIONAL_WEEKDAY, 'g'),
    new RE2(CLOCK_TIME, 'g'),
    new RE2(KOREAN_ELAPSED_TIME, 'g'),
    new RE2(ENGLISH_ELAPSED_TIME, 'gi'),
    new RE2(JUST_NOW, 'gi')
  ];

  static remove(text: string): string {
    return DateTimeRemover.PATTERNS.reduce((result, pattern) => result.replace(pattern, ''), text);
  }
}
