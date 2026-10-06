import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Attaches the JWT access token to API requests. On a 401 from a non-auth
 * endpoint, attempts a single silent refresh-token rotation and retries
 * the original request once. If rotation fails, the session is cleared.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.accessToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      const isAuthCall = req.url.includes('/api/auth/');
      const alreadyRetried = req.headers.has('X-Refresh-Retry');

      if (err.status === 401 && !isAuthCall && !alreadyRetried) {
        return auth.refresh().pipe(
          switchMap(() =>
            next(
              req.clone({
                setHeaders: {
                  Authorization: `Bearer ${auth.accessToken() ?? ''}`,
                  'X-Refresh-Retry': 'true',
                },
              }),
            ),
          ),
          catchError((refreshErr) => {
            auth.logout();
            return throwError(() => refreshErr);
          }),
        );
      }
      return throwError(() => err);
    }),
  );
};
