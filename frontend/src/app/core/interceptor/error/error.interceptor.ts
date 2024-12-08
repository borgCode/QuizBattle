import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {catchError, Observable, throwError} from 'rxjs';


@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.error?.validationErrors) {
          return throwError(() => err);
        }
        let errorMessage = 'An unexpected error occurred';
        if (err.error?.businessErrorCode) {
          switch (err.error.businessErrorCode) {
            case 305:
              errorMessage = 'Cannot send friend request to a blocked user';
              break;
            case 306:
              errorMessage = 'Friend request is already pending';
              break;
            case 307:
              errorMessage = 'You are already friends with this user';
              break;
          }
        } else if (err.status) {
          switch (err.status) {
            case 400:
              errorMessage = 'Bad request';
              break;
            case 403:
              errorMessage = 'You are not authorized to perform this action';
              break;
            case 404:
              errorMessage = 'Requested resource not found';
              break;
            case 500:
              errorMessage = 'Internal server error';
              break;
          }
        }

        throw new Error(errorMessage);

      })
    )
  }


}

