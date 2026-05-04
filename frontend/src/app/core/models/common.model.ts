export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ApiError {
  code: string;
  message: string;
  fieldErrors?: FieldIssue[];
  timestamp: string;
}

export interface FieldIssue {
  field: string;
  message: string;
}
