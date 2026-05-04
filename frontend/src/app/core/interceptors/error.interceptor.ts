import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models/common.model';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snack = inject(MatSnackBar);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const body = err.error as ApiError | null;
      const message = body?.message
        ?? (typeof err.error === 'string' ? err.error : null)
        ?? err.message
        ?? `Request failed (${err.status})`;

      const fieldMsgs = (body?.fieldErrors ?? [])
        .map(fe => `${fe.field}: ${fe.message}`)
        .join('; ');
      const full = fieldMsgs ? `${message} — ${fieldMsgs}` : message;

      snack.open(full, 'Dismiss', { duration: 6000, panelClass: 'snack-error' });
      return throwError(() => err);
    })
  );
};
