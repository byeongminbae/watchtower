"use client";

import { useId } from "react";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import type { WatchConditionInput } from "@/types/tower";

const conditionTypes = [
  { value: "HTML", label: "전체 변경 감지" },
  { value: "KEYWORD", label: "키워드 감지" },
  { value: "REGEX", label: "정규식 감지" }
];

export default function ConditionFields({ value, onChange, lockType = false }: {
  readonly value: WatchConditionInput;
  readonly onChange: (condition: WatchConditionInput) => void;
  readonly lockType?: boolean;
}) {
  const id = useId();
  return <FieldGroup>
    <Field data-disabled={lockType}>
      <FieldLabel htmlFor={`${id}-type`}>감지 조건</FieldLabel>
      <Select items={conditionTypes} value={value.type} disabled={lockType} onValueChange={type => {
        switch (type) {
          case "HTML": onChange({ type: "HTML" }); return;
          case "KEYWORD": onChange({ type: "KEYWORD", keyword: "", isSatisfiedAtAppearance: true }); return;
          case "REGEX": onChange({ type: "REGEX", regex: "", isSatisfiedAtAppearance: true }); return;
        }
      }}>
        <SelectTrigger id={`${id}-type`} className="w-full"><SelectValue /></SelectTrigger>
        <SelectContent><SelectGroup>{conditionTypes.map(type => <SelectItem key={type.value} value={type.value}>{type.label}</SelectItem>)}</SelectGroup></SelectContent>
      </Select>
    </Field>
    {value.type === "KEYWORD" && <Field><FieldLabel htmlFor={`${id}-keyword`}>키워드</FieldLabel><Input id={`${id}-keyword`} required value={value.keyword} onChange={event => onChange({ ...value, keyword: event.target.value })} /></Field>}
    {value.type === "REGEX" && <Field><FieldLabel htmlFor={`${id}-regex`}>정규식</FieldLabel><Input id={`${id}-regex`} required value={value.regex} onChange={event => onChange({ ...value, regex: event.target.value })} /></Field>}
    {value.type !== "HTML" && <Field orientation="horizontal">
      <Switch id={`${id}-appearance`} checked={value.isSatisfiedAtAppearance} onCheckedChange={checked => onChange({ ...value, isSatisfiedAtAppearance: checked })} />
      <FieldLabel htmlFor={`${id}-appearance`}>{value.isSatisfiedAtAppearance ? "등장할 때 알림" : "사라질 때 알림"}</FieldLabel>
    </Field>}
  </FieldGroup>;
}
