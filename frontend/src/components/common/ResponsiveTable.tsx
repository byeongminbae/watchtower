import type { ReactNode } from "react";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";

export default function ResponsiveTable({ headings, children }: {
  readonly headings: readonly string[];
  readonly children: ReactNode;
}) {
  return <div className="overflow-hidden [&_[data-slot=table-container]]:overflow-visible">
    <Table className="block table-fixed wrap-anywhere lg:table [&_tbody]:block lg:[&_tbody]:table-row-group [&_tbody_tr]:block [&_tbody_tr]:p-4 lg:[&_tbody_tr]:table-row lg:[&_tbody_tr]:p-0 [&_td]:block [&_td]:px-0 [&_td]:py-2 [&_td]:whitespace-normal lg:[&_td]:table-cell lg:[&_td]:px-4 lg:[&_td]:py-3.5 [&_td]:before:mb-1 [&_td]:before:block [&_td]:before:text-xs [&_td]:before:font-medium [&_td]:before:text-muted-foreground [&_td]:before:content-[attr(data-label)] lg:[&_td]:before:hidden">
      <TableHeader className="hidden bg-muted/50 lg:table-header-group"><TableRow>
        {headings.map(heading => <TableHead key={heading} className="px-4">{heading}</TableHead>)}
      </TableRow></TableHeader>
      <TableBody>{children}</TableBody>
    </Table>
  </div>;
}
