"use client";

import React from "react";
import { ArrowDown, ArrowDownWideNarrow, ArrowUp, CalendarClock, CalendarPlus, Hash, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput } from "@/components/ui/input-group";
import type { SearchCriteria } from "@/types/pagination";

const sortFields = [
  { value: "ID", label: "등록 순서", icon: Hash },
  { value: "CREATED_AT", label: "등록일", icon: CalendarPlus },
  { value: "UPDATED_AT", label: "수정일", icon: CalendarClock }
];

export default function ListControls({ value, onChange, label }: {
  readonly value: SearchCriteria;
  readonly onChange: (criteria: SearchCriteria) => void;
  readonly label: string;
}) {
  const [draft, setDraft] = React.useState(value.query);
  const id = React.useId();
  const selectedField = sortFields.find(field => field.value === value.sortField);
  return <form onSubmit={event => { event.preventDefault(); onChange({ ...value, query: draft }); }} className="flex min-w-0 items-center gap-1.5">
    <FieldGroup className="min-w-0 flex-1">
      <Field>
        <FieldLabel htmlFor={id} className="sr-only">{label}</FieldLabel>
        <InputGroup>
          <InputGroupInput id={id} value={draft} onChange={event => setDraft(event.target.value)} placeholder="이름으로 검색해요" />
          <InputGroupAddon align="inline-end"><InputGroupButton type="submit" size="icon-xs" aria-label="검색"><Search /></InputGroupButton></InputGroupAddon>
        </InputGroup>
      </Field>
    </FieldGroup>
    <DropdownMenu>
      <DropdownMenuTrigger render={<Button variant="ghost" size="icon" />} aria-label="정렬 기준" title={`정렬 기준: ${selectedField?.label}`}><ArrowDownWideNarrow /></DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup><DropdownMenuLabel>정렬 기준</DropdownMenuLabel>
          <DropdownMenuRadioGroup value={value.sortField} onValueChange={field => {
            if (field === "ID" || field === "CREATED_AT" || field === "UPDATED_AT") onChange({ ...value, sortField: field });
          }}>
            {sortFields.map(({ value: field, label: name, icon: Icon }) => <DropdownMenuRadioItem closeOnClick key={field} value={field}><Icon />{name}</DropdownMenuRadioItem>)}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
    <DropdownMenu>
      <DropdownMenuTrigger render={<Button variant="ghost" size="icon" />} aria-label="정렬 방향" title={value.sortDirection === "DESC" ? "내림차순" : "오름차순"}>{value.sortDirection === "DESC" ? <ArrowDown /> : <ArrowUp />}</DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup><DropdownMenuLabel>정렬 방향</DropdownMenuLabel>
          <DropdownMenuRadioGroup value={value.sortDirection} onValueChange={direction => {
            if (direction === "ASC" || direction === "DESC") onChange({ ...value, sortDirection: direction });
          }}>
            <DropdownMenuRadioItem closeOnClick value="DESC"><ArrowDown />내림차순</DropdownMenuRadioItem>
            <DropdownMenuRadioItem closeOnClick value="ASC"><ArrowUp />오름차순</DropdownMenuRadioItem>
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  </form>;
}
