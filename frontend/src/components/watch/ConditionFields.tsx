"use client";

import { useId } from "react";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import type { WatchConditionInput } from "@/types/tower";

export default function ConditionFields({ value, onChange }: {
  readonly value: WatchConditionInput;
  readonly onChange: (condition: WatchConditionInput) => void;
}) {
  const id = useId();
  return <FieldGroup>
    <Field><FieldLabel htmlFor={`${id}-keyword`}>키워드</FieldLabel><Input id={`${id}-keyword`} required value={value.keyword} onChange={event => onChange({ ...value, keyword: event.target.value })} /></Field>
    <Field orientation="horizontal">
      <Switch id={`${id}-appearance`} checked={value.satisfiedWhen === "APPEAR"} onCheckedChange={checked => onChange({ ...value, satisfiedWhen: checked ? "APPEAR" : "DISAPPEAR" })} />
      <FieldLabel htmlFor={`${id}-appearance`}>{value.satisfiedWhen === "APPEAR" ? "등장할 때 알림" : "사라질 때 알림"}</FieldLabel>
    </Field>
  </FieldGroup>;
}
