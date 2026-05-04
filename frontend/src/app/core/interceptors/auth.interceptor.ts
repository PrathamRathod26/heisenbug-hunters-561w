import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const user = auth.user();
  if (!user) return next(req);
  return next(req.clone({
    setHeaders: { 'X-User-Id': user.id }
  }));
};
