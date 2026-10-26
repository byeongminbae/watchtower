export type SearchCriteria = {
  readonly query: string;
  readonly size: number;
  readonly sortField: "ID" | "CREATED_AT" | "UPDATED_AT";
  readonly sortDirection: "ASC" | "DESC";
};
export const defaultSearch: SearchCriteria = { query: "", size: 20, sortField: "ID", sortDirection: "DESC" };
export type Cursor = { readonly id: number; readonly value: string | null };
export type CursorPage<T> = {
  readonly data: readonly T[];
  readonly size: number;
  readonly nextCursorId: number | null;
  readonly nextCursorValue: string | null;
  readonly hasNext: boolean;
};
export type OffsetPage<T> = {
  readonly data: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
};
