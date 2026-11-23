import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Envia o token Bearer e encerra a sessão quando a API responde 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;

  const requisicao = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}`, Accept: 'application/json' } })
    : req.clone({ setHeaders: { Accept: 'application/json' } });

  return next(requisicao).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status === 401 && !req.url.endsWith('/login')) {
        auth.limparSessao();
      }
      return throwError(() => erro);
    }),
  );
};
