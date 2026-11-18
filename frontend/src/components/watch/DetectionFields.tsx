"use client";

import { useId } from "react";
import { Field, FieldContent, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import type { WatchDetection } from "@/types/tower";
import { MAX_DETECTION_AREA_SELECTOR_LENGTH, toDetectionAreaSelector } from "@/lib/tower";

/** @param detecting 감지 조건까지 포함해 알림을 받을 변화가 하나라도 있는지. false 면 저장할 수 없는 이유를 보여줌 */
export default function DetectionFields({ value, onChange, detecting }: {
  readonly value: WatchDetection;
  readonly onChange: (detection: WatchDetection) => void;
  readonly detecting: boolean;
}) {
  const id = useId();
  return <FieldSet data-invalid={!detecting}>
    <FieldLegend variant="label">감지 방식</FieldLegend>
    <FieldDescription>켜 둔 항목 중 하나라도 바뀌면 알려 드려요.</FieldDescription>
    <FieldGroup>
      <Field>
        <FieldLabel htmlFor={`${id}-area`}>감시할 영역 (선택)</FieldLabel>
        <Input id={`${id}-area`} maxLength={MAX_DETECTION_AREA_SELECTOR_LENGTH} value={value.detectionAreaSelector ?? ""} placeholder="예: .price, #stock"
          onChange={event => onChange({ ...value, detectionAreaSelector: toDetectionAreaSelector(event.target.value) })} />
        <FieldDescription>CSS 선택자로 고른 영역 안에서만 변화를 확인해요. 비워 두면 페이지 전체를 확인해요.</FieldDescription>
      </Field>
      <Field orientation="horizontal">
        <FieldContent>
          <FieldLabel htmlFor={`${id}-text`}>텍스트 변화</FieldLabel>
          <FieldDescription>페이지의 글자가 바뀌면 알려 드려요.</FieldDescription>
        </FieldContent>
        <Switch id={`${id}-text`} checked={value.detectTextChange} onCheckedChange={checked => onChange({ ...value, detectTextChange: checked })} />
      </Field>
      <Field orientation="horizontal">
        <FieldContent>
          <FieldLabel htmlFor={`${id}-image`}>이미지 변화</FieldLabel>
          <FieldDescription>페이지의 이미지가 바뀌면 알려 드려요.</FieldDescription>
        </FieldContent>
        <Switch id={`${id}-image`} checked={value.detectImageChange} onCheckedChange={checked => onChange({ ...value, detectImageChange: checked })} />
      </Field>
      <Field orientation="horizontal">
        <FieldContent>
          <FieldLabel htmlFor={`${id}-date-time`}>날짜와 시간 표기 무시</FieldLabel>
          <FieldDescription>방문 시각처럼 날짜와 시간만 바뀐 변화는 알리지 않아요.</FieldDescription>
        </FieldContent>
        <Switch id={`${id}-date-time`} checked={value.ignoreDateTime} onCheckedChange={checked => onChange({ ...value, ignoreDateTime: checked })} />
      </Field>
    </FieldGroup>
    {!detecting && <FieldError>텍스트나 이미지 변화를 켜거나 감지 조건을 하나 이상 추가해 주세요.</FieldError>}
  </FieldSet>;
}
