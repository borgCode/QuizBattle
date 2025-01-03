import {
  HttpEvent,
  HttpHandler,
  HttpHeaders,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {TokenService} from '../../services/token/token.service';
import {catchError, from, Observable, switchMap, throwError} from 'rxjs';

@Injectable()
export class HttpTokenInterceptor implements HttpInterceptor {
  private isRefreshing = false;

  constructor(
    private tokenService: TokenService
  ) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const accessToken = this.tokenService.accessToken;

    if (accessToken) {
      req = this.addToken(req, accessToken);
    }

    return next.handle(req).pipe(
      catchError(err => {
        if (err.status === 401 && !req.url.includes("auth/refresh")) {
          return this.handle401Error(req, next);
        }
        return throwError(() => err);
      })
    );
  }

  private addToken(req: HttpRequest<any>, accessToken: string) {
    return req.clone({
      headers: new HttpHeaders({
        Authorization: 'Bearer ' + accessToken
      })
    });
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;

      return from(this.tokenService.refreshAccessToken()).pipe(
        switchMap(success => {
          this.isRefreshing = false;
          if (success) {
            return next.handle(this.addToken(req, this.tokenService.accessToken));
          }
          return throwError(() => new Error("Token refresh failed"))
        }),
        catchError(err => {
          this.isRefreshing = false;
          return throwError(() => err);
        })
      )
    }
    return next.handle(req);
  }
}


